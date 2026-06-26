package com.edulife.exams.service;

import com.edulife.certificates.dto.CertificateDetailDto;
import com.edulife.certificates.service.CertificateService;
import com.edulife.enrollments.model.EnrollmentStatus;
import com.edulife.enrollments.repository.EnrollmentRepository;
import com.edulife.exams.dto.ExamDto;
import com.edulife.exams.dto.ExamResultDto;
import com.edulife.exams.dto.ExamStatusDto;
import com.edulife.exams.dto.SubmitExamRequest;
import com.edulife.exams.entity.Exam;
import com.edulife.exams.entity.ExamAttempt;
import com.edulife.exams.entity.ExamChoice;
import com.edulife.exams.entity.ExamQuestion;
import com.edulife.exams.exception.ExamAlreadyPassedException;
import com.edulife.exams.exception.ExamCooldownException;
import com.edulife.exams.repository.ExamAttemptRepository;
import com.edulife.exams.repository.ExamChoiceRepository;
import com.edulife.exams.repository.ExamQuestionRepository;
import com.edulife.exams.repository.ExamRepository;
import com.edulife.gamification.service.GamificationService;
import com.edulife.security.FirebaseAuthentication;
import com.edulife.users.entity.User;
import com.edulife.users.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Core service for exam lifecycle: retrieval, status checks, and submission with server-side scoring.
 *
 * <p>Business rules enforced: pass threshold is 80%, 2 consecutive failures trigger a 72-hour cooldown,
 * correct answers are never exposed to clients, and certificates are issued only upon passing.</p>
 */
@Service
public class ExamService {

    private final ExamRepository examRepository;
    private final ExamQuestionRepository questionRepository;
    private final ExamChoiceRepository choiceRepository;
    private final ExamAttemptRepository attemptRepository;
    private final CertificateService certificateService;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final GamificationService gamificationService;

    public ExamService(
            ExamRepository examRepository,
            ExamQuestionRepository questionRepository,
            ExamChoiceRepository choiceRepository,
            ExamAttemptRepository attemptRepository,
            CertificateService certificateService,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository,
            GamificationService gamificationService) {
        this.examRepository = examRepository;
        this.questionRepository = questionRepository;
        this.choiceRepository = choiceRepository;
        this.attemptRepository = attemptRepository;
        this.certificateService = certificateService;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.gamificationService = gamificationService;
    }

    /**
     * Retrieves the exam for a course with questions and shuffled choices.
     * Requires the current user to have an active enrollment. Correct answers are excluded from the response.
     */
    public ExamDto getExam(UUID courseId) {
        User user = resolveCurrentUser();

        boolean enrolled = enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                user.getId(), courseId, EnrollmentStatus.ACTIVE);
        if (!enrolled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Enroll in this course to access the exam");
        }

        Exam exam = examRepository.findByCourseId(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No exam found for this course"));

        List<ExamQuestion> questions = questionRepository.findAllByExamIdOrderByOrderIndexAsc(exam.getId());
        List<UUID> questionIds = questions.stream().map(ExamQuestion::getId).toList();

        Map<UUID, List<ExamChoice>> choicesByQuestion = choiceRepository
                .findAllByQuestionIdIn(questionIds)
                .stream()
                .collect(Collectors.groupingBy(ExamChoice::getQuestionId));

        List<ExamDto.QuestionDto> questionDtos = questions.stream().map(q -> {
            List<ExamChoice> choices = choicesByQuestion.getOrDefault(q.getId(), List.of());
            List<ExamChoice> shuffled = new java.util.ArrayList<>(choices);
            Collections.shuffle(shuffled);
            List<ExamDto.ChoiceDto> choiceDtos = shuffled.stream()
                    .map(c -> new ExamDto.ChoiceDto(c.getId(), c.getChoiceText()))
                    .toList();
            return new ExamDto.QuestionDto(q.getId(), q.getQuestionText(), q.getOrderIndex(), choiceDtos);
        }).toList();

        return new ExamDto(exam.getId(), exam.getCourseId(), exam.getTitle(),
                exam.getPassScore(), exam.getTimeLimitMinutes(), questionDtos);
    }

    /**
     * Returns the learner's exam status including pass state, failure count, and 72-hour cooldown window.
     */
    public ExamStatusDto getExamStatus(UUID courseId) {
        User user = resolveCurrentUser();

        boolean enrolled = enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                user.getId(), courseId, EnrollmentStatus.ACTIVE);
        if (!enrolled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Enroll in this course to view exam status");
        }

        Exam exam = examRepository.findByCourseId(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No exam found for this course"));

        boolean passed = attemptRepository.existsByUserIdAndExamIdAndPassedTrue(user.getId(), exam.getId());
        long failedAttempts = attemptRepository.countByUserIdAndExamIdAndPassedFalse(user.getId(), exam.getId());

        boolean inCooldown = false;
        Instant cooldownEndsAt = null;

        if (!passed && failedAttempts >= 2) {
            ExamAttempt lastFailure = attemptRepository
                    .findTopByUserIdAndExamIdAndPassedFalseOrderByTakenAtDesc(user.getId(), exam.getId())
                    .orElseThrow();
            Instant expiry = lastFailure.getTakenAt().plus(72, ChronoUnit.HOURS);
            if (Instant.now().isBefore(expiry)) {
                inCooldown = true;
                cooldownEndsAt = expiry;
            }
        }

        return new ExamStatusDto(exam.getId(), passed, (int) failedAttempts, 2, inCooldown, cooldownEndsAt);
    }

    /**
     * Scores an exam submission server-side, records the attempt, and issues a certificate on pass.
     *
     * <p>Enforces enrollment, already-passed, and cooldown guards. On pass, awards gamification XP
     * and delegates certificate generation to {@link CertificateService}.</p>
     */
    @Transactional
    public ExamResultDto submitExam(UUID courseId, SubmitExamRequest request) {
        User user = resolveCurrentUser();

        boolean enrolled = enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                user.getId(), courseId, EnrollmentStatus.ACTIVE);
        if (!enrolled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Enroll in this course to submit the exam");
        }

        Exam exam = examRepository.findByCourseId(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No exam found for this course"));

        if (attemptRepository.existsByUserIdAndExamIdAndPassedTrue(user.getId(), exam.getId())) {
            throw new ExamAlreadyPassedException();
        }

        long failedCount = attemptRepository.countByUserIdAndExamIdAndPassedFalse(user.getId(), exam.getId());
        if (failedCount >= 2) {
            ExamAttempt lastFailure = attemptRepository
                    .findTopByUserIdAndExamIdAndPassedFalseOrderByTakenAtDesc(user.getId(), exam.getId())
                    .orElseThrow();
            Instant cooldownEndsAt = lastFailure.getTakenAt().plus(72, ChronoUnit.HOURS);
            if (Instant.now().isBefore(cooldownEndsAt)) {
                throw new ExamCooldownException(cooldownEndsAt);
            }
        }

        List<ExamQuestion> questions = questionRepository.findAllByExamIdOrderByOrderIndexAsc(exam.getId());
        Set<UUID> validQuestionIds = questions.stream().map(ExamQuestion::getId).collect(Collectors.toSet());

        for (SubmitExamRequest.AnswerDto answer : request.answers()) {
            if (!validQuestionIds.contains(answer.questionId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Question " + answer.questionId() + " does not belong to this exam");
            }
        }

        List<UUID> questionIds = questions.stream().map(ExamQuestion::getId).toList();
        Map<UUID, ExamChoice> choiceById = choiceRepository
                .findAllByQuestionIdIn(questionIds)
                .stream()
                .collect(Collectors.toMap(ExamChoice::getId, c -> c));

        int correct = 0;
        for (SubmitExamRequest.AnswerDto answer : request.answers()) {
            ExamChoice choice = choiceById.get(answer.choiceId());
            if (choice != null && choice.getQuestionId().equals(answer.questionId()) && choice.isCorrect()) {
                correct++;
            }
        }

        int total = questions.size();
        int score = total == 0 ? 0 : (int) Math.round((correct * 100.0) / total);
        boolean passed = score >= exam.getPassScore();

        ExamAttempt savedAttempt;
        try {
            // Flush immediately so the one-pass-per-user unique index (V27) is enforced inside this
            // transaction. If a concurrent submission already committed a passing attempt, this insert
            // violates the index and we report the normal "already passed" result instead of awarding
            // XP and issuing a certificate twice.
            savedAttempt = attemptRepository.saveAndFlush(new ExamAttempt(user.getId(), exam.getId(), score, passed));
        } catch (DataIntegrityViolationException e) {
            if (passed) {
                throw new ExamAlreadyPassedException();
            }
            throw e;
        }

        String certificateNumber = null;
        if (passed) {
            // Certificates are issued only from the server-side final-exam pass branch;
            // lesson completion alone must never create a certificate.
            // XP for the pass is awarded before the certificate so the dedup key tied to the
            // exam attempt id is recorded even if certificate generation fails downstream.
            gamificationService.onExamPassed(user.getId(), savedAttempt.getId());

            CertificateDetailDto cert = certificateService.generateCertificateAfterExamPass(
                    user.getId(), courseId, savedAttempt.getId());
            certificateNumber = cert.certificateNumber();
            gamificationService.onCertificateEarned(user.getId(), cert.id());
        }

        long totalFailed = attemptRepository.countByUserIdAndExamIdAndPassedFalse(user.getId(), exam.getId());
        Instant cooldownEndsAt = null;
        if (!passed && totalFailed >= 2) {
            cooldownEndsAt = savedAttempt.getTakenAt().plus(72, ChronoUnit.HOURS);
        }

        return new ExamResultDto(exam.getId(), score, exam.getPassScore(), passed, certificateNumber,
                (int) totalFailed, cooldownEndsAt);
    }

    /** Resolves the current authenticated user from the Firebase security context. */
    private User resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof FirebaseAuthentication firebaseAuth)) {
            throw new IllegalStateException("Firebase authentication required");
        }
        return userRepository.findByFirebaseUid(firebaseAuth.getFirebaseUid())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "User not found. Call /auth/sync first."));
    }
}

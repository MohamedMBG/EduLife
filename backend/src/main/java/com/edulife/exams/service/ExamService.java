package com.edulife.exams.service;

import com.edulife.certificates.entity.Certificate;
import com.edulife.certificates.repository.CertificateRepository;
import com.edulife.enrollments.model.EnrollmentStatus;
import com.edulife.enrollments.repository.EnrollmentRepository;
import com.edulife.exams.dto.ExamDto;
import com.edulife.exams.dto.ExamResultDto;
import com.edulife.exams.dto.SubmitExamRequest;
import com.edulife.exams.entity.Exam;
import com.edulife.exams.entity.ExamAttempt;
import com.edulife.exams.entity.ExamChoice;
import com.edulife.exams.entity.ExamQuestion;
import com.edulife.exams.repository.ExamAttemptRepository;
import com.edulife.exams.repository.ExamChoiceRepository;
import com.edulife.exams.repository.ExamQuestionRepository;
import com.edulife.exams.repository.ExamRepository;
import com.edulife.security.FirebaseAuthentication;
import com.edulife.users.entity.User;
import com.edulife.users.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.time.Year;
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

@Service
public class ExamService {

    private final ExamRepository examRepository;
    private final ExamQuestionRepository questionRepository;
    private final ExamChoiceRepository choiceRepository;
    private final ExamAttemptRepository attemptRepository;
    private final CertificateRepository certificateRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    public ExamService(
            ExamRepository examRepository,
            ExamQuestionRepository questionRepository,
            ExamChoiceRepository choiceRepository,
            ExamAttemptRepository attemptRepository,
            CertificateRepository certificateRepository,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository) {
        this.examRepository = examRepository;
        this.questionRepository = questionRepository;
        this.choiceRepository = choiceRepository;
        this.attemptRepository = attemptRepository;
        this.certificateRepository = certificateRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
    }

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
            // Shuffle choices so correct answer position varies between attempts
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

        List<ExamQuestion> questions = questionRepository.findAllByExamIdOrderByOrderIndexAsc(exam.getId());
        Set<UUID> validQuestionIds = questions.stream().map(ExamQuestion::getId).collect(Collectors.toSet());

        // Validate all submitted questionIds belong to this exam
        for (SubmitExamRequest.AnswerDto answer : request.answers()) {
            if (!validQuestionIds.contains(answer.questionId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Question " + answer.questionId() + " does not belong to this exam");
            }
        }

        // Bulk-load all choices for the exam questions
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

        attemptRepository.save(new ExamAttempt(user.getId(), exam.getId(), score, passed));

        // Issue certificate on first pass — idempotent
        String certificateNumber = null;
        if (passed && !certificateRepository.existsByUserIdAndCourseId(user.getId(), courseId)) {
            certificateNumber = generateCertificateNumber();
            certificateRepository.save(new Certificate(user.getId(), courseId, certificateNumber));
        }

        return new ExamResultDto(exam.getId(), score, exam.getPassScore(), passed, certificateNumber);
    }

    private String generateCertificateNumber() {
        String year = String.valueOf(Year.now().getValue());
        String unique = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return "EL-" + year + "-" + unique;
    }

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

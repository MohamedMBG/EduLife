package com.edulife.admin.service;

import com.edulife.admin.dto.CreateExamRequest;
import com.edulife.admin.dto.ExamAdminDto;
import com.edulife.courses.repository.CourseRepository;
import com.edulife.exams.entity.Exam;
import com.edulife.exams.entity.ExamChoice;
import com.edulife.exams.entity.ExamQuestion;
import com.edulife.exams.repository.ExamChoiceRepository;
import com.edulife.exams.repository.ExamQuestionRepository;
import com.edulife.exams.repository.ExamRepository;
import com.edulife.security.FirebaseAuthentication;
import com.edulife.users.entity.User;
import com.edulife.users.model.UserRole;
import com.edulife.users.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * CMS exam authoring. The entire exam — including all questions and choices — is
 * created atomically in a single transaction so a partial save cannot leave an
 * exam with missing questions that learners would see as an empty exam.
 */
@Service
public class CmsExamService {

    private final ExamRepository examRepository;
    private final ExamQuestionRepository questionRepository;
    private final ExamChoiceRepository choiceRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public CmsExamService(
            ExamRepository examRepository,
            ExamQuestionRepository questionRepository,
            ExamChoiceRepository choiceRepository,
            CourseRepository courseRepository,
            UserRepository userRepository
    ) {
        this.examRepository = examRepository;
        this.questionRepository = questionRepository;
        this.choiceRepository = choiceRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    // Includes correct-answer flags — this endpoint is admin/teacher only, not learner-facing.
    public ExamAdminDto getExam(UUID courseId) {
        User currentUser = resolveCurrentUser();
        loadCourseForRead(courseId, currentUser);

        Exam exam = examRepository.findByCourseId(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No exam found for this course"));

        List<ExamQuestion> questions = questionRepository.findAllByExamIdOrderByOrderIndexAsc(exam.getId());
        List<UUID> questionIds = questions.stream().map(ExamQuestion::getId).toList();

        Map<UUID, List<ExamChoice>> choicesByQuestion = choiceRepository
                .findAllByQuestionIdIn(questionIds)
                .stream()
                .collect(Collectors.groupingBy(ExamChoice::getQuestionId));

        List<ExamAdminDto.QuestionDto> questionDtos = questions.stream().map(q -> {
            List<ExamAdminDto.ChoiceDto> choices = choicesByQuestion
                    .getOrDefault(q.getId(), List.of())
                    .stream()
                    .map(c -> new ExamAdminDto.ChoiceDto(c.getId(), c.getChoiceText(), c.isCorrect()))
                    .toList();
            return new ExamAdminDto.QuestionDto(q.getId(), q.getQuestionText(), q.getOrderIndex(), choices);
        }).toList();

        return new ExamAdminDto(exam.getId(), exam.getCourseId(), exam.getTitle(),
                exam.getPassScore(), exam.getTimeLimitMinutes(), questionDtos);
    }

    @Transactional
    // Atomic creation: exam + all questions + all choices in one transaction.
    // If any validation fails mid-loop, the whole transaction rolls back.
    public ExamAdminDto createExam(UUID courseId, CreateExamRequest request) {
        User currentUser = resolveCurrentUser();
        loadCourseForMutation(courseId, currentUser);

        // One exam per course — the schema enforces this with a UNIQUE constraint on course_id.
        if (examRepository.findByCourseId(courseId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An exam already exists for this course");
        }

        // Validate that every question has exactly one correct choice to prevent unscoreable exams.
        for (CreateExamRequest.QuestionRequest qr : request.questions()) {
            long correctCount = qr.choices().stream().filter(CreateExamRequest.ChoiceRequest::correct).count();
            if (correctCount != 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Question '" + qr.questionText() + "' must have exactly one correct choice");
            }
        }

        Exam exam = examRepository.save(
                new Exam(courseId, request.title(), request.passScore(), request.timeLimitMinutes())
        );

        List<ExamAdminDto.QuestionDto> questionDtos = new ArrayList<>();

        for (CreateExamRequest.QuestionRequest qr : request.questions()) {
            ExamQuestion question = questionRepository.save(
                    new ExamQuestion(exam.getId(), qr.questionText(), qr.orderIndex())
            );

            List<ExamAdminDto.ChoiceDto> choiceDtos = new ArrayList<>();
            for (CreateExamRequest.ChoiceRequest cr : qr.choices()) {
                ExamChoice choice = choiceRepository.save(
                        new ExamChoice(question.getId(), cr.choiceText(), cr.correct())
                );
                choiceDtos.add(new ExamAdminDto.ChoiceDto(choice.getId(), choice.getChoiceText(), choice.isCorrect()));
            }

            questionDtos.add(new ExamAdminDto.QuestionDto(
                    question.getId(), question.getQuestionText(), question.getOrderIndex(), choiceDtos));
        }

        return new ExamAdminDto(exam.getId(), exam.getCourseId(), exam.getTitle(),
                exam.getPassScore(), exam.getTimeLimitMinutes(), questionDtos);
    }

    @Transactional
    public ExamAdminDto updateExam(UUID courseId, CreateExamRequest request) {
        User currentUser = resolveCurrentUser();
        loadCourseForMutation(courseId, currentUser);

        Exam exam = examRepository.findByCourseId(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No exam found for this course"));

        for (CreateExamRequest.QuestionRequest qr : request.questions()) {
            long correctCount = qr.choices().stream().filter(CreateExamRequest.ChoiceRequest::correct).count();
            if (correctCount != 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Question '" + qr.questionText() + "' must have exactly one correct choice");
            }
        }

        List<ExamQuestion> oldQuestions = questionRepository.findAllByExamIdOrderByOrderIndexAsc(exam.getId());
        List<UUID> oldQuestionIds = oldQuestions.stream().map(ExamQuestion::getId).toList();
        if (!oldQuestionIds.isEmpty()) {
            choiceRepository.deleteAllByQuestionIdIn(oldQuestionIds);
        }
        questionRepository.deleteAllByExamId(exam.getId());

        exam.setTitle(request.title());
        exam.setPassScore(request.passScore());
        exam.setTimeLimitMinutes(request.timeLimitMinutes());
        examRepository.save(exam);

        List<ExamAdminDto.QuestionDto> questionDtos = new ArrayList<>();

        for (CreateExamRequest.QuestionRequest qr : request.questions()) {
            ExamQuestion question = questionRepository.save(
                    new ExamQuestion(exam.getId(), qr.questionText(), qr.orderIndex())
            );

            List<ExamAdminDto.ChoiceDto> choiceDtos = new ArrayList<>();
            for (CreateExamRequest.ChoiceRequest cr : qr.choices()) {
                ExamChoice choice = choiceRepository.save(
                        new ExamChoice(question.getId(), cr.choiceText(), cr.correct())
                );
                choiceDtos.add(new ExamAdminDto.ChoiceDto(choice.getId(), choice.getChoiceText(), choice.isCorrect()));
            }

            questionDtos.add(new ExamAdminDto.QuestionDto(
                    question.getId(), question.getQuestionText(), question.getOrderIndex(), choiceDtos));
        }

        return new ExamAdminDto(exam.getId(), exam.getCourseId(), exam.getTitle(),
                exam.getPassScore(), exam.getTimeLimitMinutes(), questionDtos);
    }

    @Transactional
    public void deleteExam(UUID courseId) {
        User currentUser = resolveCurrentUser();
        loadCourseForMutation(courseId, currentUser);

        Exam exam = examRepository.findByCourseId(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No exam found for this course"));

        List<ExamQuestion> questions = questionRepository.findAllByExamIdOrderByOrderIndexAsc(exam.getId());
        List<UUID> questionIds = questions.stream().map(ExamQuestion::getId).toList();
        if (!questionIds.isEmpty()) {
            choiceRepository.deleteAllByQuestionIdIn(questionIds);
        }
        questionRepository.deleteAllByExamId(exam.getId());
        examRepository.delete(exam);
    }

    private void loadCourseForRead(UUID courseId, User currentUser) {
        courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    }

    private void loadCourseForMutation(UUID courseId, User currentUser) {
        var course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        boolean isOwner = course.getCreatedByUserId() != null
                && course.getCreatedByUserId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not the course owner");
        }
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

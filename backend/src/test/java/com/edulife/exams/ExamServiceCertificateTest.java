package com.edulife.exams;

import com.edulife.certificates.dto.CertificateDetailDto;
import com.edulife.certificates.service.CertificateService;
import com.edulife.enrollments.model.EnrollmentStatus;
import com.edulife.enrollments.repository.EnrollmentRepository;
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
import com.edulife.exams.service.ExamService;
import com.edulife.gamification.service.GamificationService;
import com.edulife.security.FirebaseAuthentication;
import com.edulife.users.entity.User;
import com.edulife.users.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExamServiceCertificateTest {

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID COURSE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID EXAM_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID QUESTION_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID CORRECT_CHOICE_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final UUID WRONG_CHOICE_ID = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
    private static final UUID ATTEMPT_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID CERT_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Mock private ExamRepository examRepository;
    @Mock private ExamQuestionRepository questionRepository;
    @Mock private ExamChoiceRepository choiceRepository;
    @Mock private ExamAttemptRepository attemptRepository;
    @Mock private CertificateService certificateService;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private GamificationService gamificationService;

    private ExamService examService;

    @BeforeEach
    void setUp() {
        examService = new ExamService(
                examRepository,
                questionRepository,
                choiceRepository,
                attemptRepository,
                certificateService,
                enrollmentRepository,
                userRepository,
                gamificationService
        );
        SecurityContextHolder.getContext().setAuthentication(
                new FirebaseAuthentication("firebase-uid", "learner@edulife.test")
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitExamGeneratesCertificateOnlyWhenPassed() {
        givenExamWithOneQuestion();
        given(attemptRepository.save(any(ExamAttempt.class))).willAnswer(invocation -> savedAttempt(invocation.getArgument(0)));
        given(certificateService.generateCertificateAfterExamPass(USER_ID, COURSE_ID, ATTEMPT_ID))
                .willReturn(new CertificateDetailDto(
                        CERT_ID,
                        COURSE_ID,
                        "EL-2026-PASS",
                        "Jane Learner",
                        "Prof. Teacher",
                        "Algebra Foundations",
                        "BEGINNER",
                        Instant.parse("2026-06-16T10:00:00Z"),
                        "verification-hash",
                        "storage/certificates/certificate.pdf"
                ));

        ExamResultDto result = examService.submitExam(COURSE_ID,
                new SubmitExamRequest(List.of(new SubmitExamRequest.AnswerDto(QUESTION_ID, CORRECT_CHOICE_ID))));

        assertThat(result.passed()).isTrue();
        assertThat(result.certificateNumber()).isEqualTo("EL-2026-PASS");
        // The certificate is issued from the server-side pass branch, never from lesson progress.
        verify(certificateService).generateCertificateAfterExamPass(USER_ID, COURSE_ID, ATTEMPT_ID);
    }

    @Test
    void submitExamDoesNotGenerateCertificateWhenFailed() {
        givenExamWithOneQuestion();
        given(attemptRepository.save(any(ExamAttempt.class))).willAnswer(invocation -> savedAttempt(invocation.getArgument(0)));

        ExamResultDto result = examService.submitExam(COURSE_ID,
                new SubmitExamRequest(List.of(new SubmitExamRequest.AnswerDto(QUESTION_ID, WRONG_CHOICE_ID))));

        assertThat(result.passed()).isFalse();
        assertThat(result.certificateNumber()).isNull();
        verify(certificateService, never()).generateCertificateAfterExamPass(any(), any(), any());
        verify(gamificationService, never()).onCertificateEarned(any(), any());
    }

    private void givenExamWithOneQuestion() {
        User user = mock(User.class);
        given(user.getId()).willReturn(USER_ID);
        given(userRepository.findByFirebaseUid("firebase-uid")).willReturn(Optional.of(user));
        given(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(USER_ID, COURSE_ID, EnrollmentStatus.ACTIVE))
                .willReturn(true);

        Exam exam = new Exam(COURSE_ID, "Final exam", 80, null);
        ReflectionTestUtils.setField(exam, "id", EXAM_ID);
        given(examRepository.findByCourseId(COURSE_ID)).willReturn(Optional.of(exam));
        given(attemptRepository.existsByUserIdAndExamIdAndPassedTrue(USER_ID, EXAM_ID)).willReturn(false);
        given(attemptRepository.countByUserIdAndExamIdAndPassedFalse(USER_ID, EXAM_ID)).willReturn(0L);

        ExamQuestion question = new ExamQuestion(EXAM_ID, "What is 2 + 2?", 1);
        ReflectionTestUtils.setField(question, "id", QUESTION_ID);
        given(questionRepository.findAllByExamIdOrderByOrderIndexAsc(EXAM_ID)).willReturn(List.of(question));

        ExamChoice correctChoice = new ExamChoice(QUESTION_ID, "4", true);
        ReflectionTestUtils.setField(correctChoice, "id", CORRECT_CHOICE_ID);
        ExamChoice wrongChoice = new ExamChoice(QUESTION_ID, "5", false);
        ReflectionTestUtils.setField(wrongChoice, "id", WRONG_CHOICE_ID);
        given(choiceRepository.findAllByQuestionIdIn(List.of(QUESTION_ID))).willReturn(List.of(correctChoice, wrongChoice));
    }

    private ExamAttempt savedAttempt(ExamAttempt attempt) {
        ReflectionTestUtils.setField(attempt, "id", ATTEMPT_ID);
        ReflectionTestUtils.setField(attempt, "takenAt", Instant.parse("2026-06-16T10:00:00Z"));
        return attempt;
    }
}

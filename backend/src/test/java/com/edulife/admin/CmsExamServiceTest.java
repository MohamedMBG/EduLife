package com.edulife.admin;

import com.edulife.admin.dto.CreateExamRequest;
import com.edulife.admin.dto.ExamAdminDto;
import com.edulife.admin.service.CmsCourseAccessGuard;
import com.edulife.admin.service.CmsExamService;
import com.edulife.courses.entity.Course;
import com.edulife.courses.repository.CourseRepository;
import com.edulife.exams.entity.Exam;
import com.edulife.exams.entity.ExamChoice;
import com.edulife.exams.entity.ExamQuestion;
import com.edulife.exams.repository.ExamChoiceRepository;
import com.edulife.exams.repository.ExamQuestionRepository;
import com.edulife.exams.repository.ExamRepository;
import com.edulife.groups.repository.GroupMemberRepository;
import com.edulife.security.FirebaseAuthentication;
import com.edulife.users.entity.User;
import com.edulife.users.model.UserRole;
import com.edulife.users.repository.UserRepository;
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
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CmsExamServiceTest {

    private static final UUID OWNER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID OTHER_USER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID COURSE_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID EXAM_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID Q1_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");

    @Mock private ExamRepository examRepository;
    @Mock private ExamQuestionRepository questionRepository;
    @Mock private ExamChoiceRepository choiceRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private UserRepository userRepository;
    @Mock private GroupMemberRepository groupMemberRepository;

    private CmsExamService cmsExamService;

    @BeforeEach
    void setUp() {
        cmsExamService = new CmsExamService(
                examRepository, questionRepository, choiceRepository,
                courseRepository, userRepository,
                new CmsCourseAccessGuard(groupMemberRepository)
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ownerCanUpdateExam() {
        setUpAuth("owner-uid", OWNER_ID, UserRole.TEACHER);
        givenCourseOwnedBy(OWNER_ID);
        givenExistingExamWithQuestions();
        givenSaveReturnsInput();

        ExamAdminDto result = cmsExamService.updateExam(COURSE_ID, validRequest());

        assertThat(result.title()).isEqualTo("Updated Exam");
        verify(choiceRepository).deleteAllByQuestionIdIn(List.of(Q1_ID));
        verify(questionRepository).deleteAllByExamId(EXAM_ID);
    }

    @Test
    void nonOwnerCannotUpdateExam() {
        setUpAuth("other-uid", OTHER_USER_ID, UserRole.TEACHER);
        givenCourseOwnedBy(OWNER_ID);

        assertThatThrownBy(() -> cmsExamService.updateExam(COURSE_ID, validRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Not the course owner");
    }

    @Test
    void adminCanUpdateAnyExam() {
        setUpAuth("admin-uid", OTHER_USER_ID, UserRole.ADMIN);
        givenCourseOwnedBy(OWNER_ID);
        givenExistingExamWithQuestions();
        givenSaveReturnsInput();

        ExamAdminDto result = cmsExamService.updateExam(COURSE_ID, validRequest());

        assertThat(result.title()).isEqualTo("Updated Exam");
    }

    @Test
    void updateWithNoExamReturns404() {
        setUpAuth("owner-uid", OWNER_ID, UserRole.TEACHER);
        givenCourseOwnedBy(OWNER_ID);
        given(examRepository.findByCourseId(COURSE_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> cmsExamService.updateExam(COURSE_ID, validRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No exam found");
    }

    @Test
    void updateReplacesOldQuestionsAndChoices() {
        setUpAuth("owner-uid", OWNER_ID, UserRole.TEACHER);
        givenCourseOwnedBy(OWNER_ID);
        givenExistingExamWithQuestions();
        givenSaveReturnsInput();

        cmsExamService.updateExam(COURSE_ID, validRequest());

        verify(choiceRepository).deleteAllByQuestionIdIn(List.of(Q1_ID));
        verify(questionRepository).deleteAllByExamId(EXAM_ID);
        verify(questionRepository).save(any(ExamQuestion.class));
        verify(choiceRepository, atLeastOnce()).save(any(ExamChoice.class));
    }

    @Test
    void updateWithZeroCorrectChoicesReturns400() {
        setUpAuth("owner-uid", OWNER_ID, UserRole.TEACHER);
        givenCourseOwnedBy(OWNER_ID);
        givenExistingExamEntity();

        CreateExamRequest bad = new CreateExamRequest("Exam", 80, 30,
                List.of(new CreateExamRequest.QuestionRequest("Q1?", 1,
                        List.of(new CreateExamRequest.ChoiceRequest("A", false),
                                new CreateExamRequest.ChoiceRequest("B", false)))));

        assertThatThrownBy(() -> cmsExamService.updateExam(COURSE_ID, bad))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("exactly one correct choice");
    }

    @Test
    void updateWithMultipleCorrectChoicesReturns400() {
        setUpAuth("owner-uid", OWNER_ID, UserRole.TEACHER);
        givenCourseOwnedBy(OWNER_ID);
        givenExistingExamEntity();

        CreateExamRequest bad = new CreateExamRequest("Exam", 80, 30,
                List.of(new CreateExamRequest.QuestionRequest("Q1?", 1,
                        List.of(new CreateExamRequest.ChoiceRequest("A", true),
                                new CreateExamRequest.ChoiceRequest("B", true)))));

        assertThatThrownBy(() -> cmsExamService.updateExam(COURSE_ID, bad))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("exactly one correct choice");
    }

    @Test
    void ownerCanDeleteExam() {
        setUpAuth("owner-uid", OWNER_ID, UserRole.TEACHER);
        givenCourseOwnedBy(OWNER_ID);
        givenExistingExamWithQuestions();

        cmsExamService.deleteExam(COURSE_ID);

        verify(choiceRepository).deleteAllByQuestionIdIn(List.of(Q1_ID));
        verify(questionRepository).deleteAllByExamId(EXAM_ID);
        verify(examRepository).delete(any(Exam.class));
    }

    @Test
    void nonOwnerCannotDeleteExam() {
        setUpAuth("other-uid", OTHER_USER_ID, UserRole.TEACHER);
        givenCourseOwnedBy(OWNER_ID);

        assertThatThrownBy(() -> cmsExamService.deleteExam(COURSE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Not the course owner");
    }

    @Test
    void deleteWithNoExamReturns404() {
        setUpAuth("owner-uid", OWNER_ID, UserRole.TEACHER);
        givenCourseOwnedBy(OWNER_ID);
        given(examRepository.findByCourseId(COURSE_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> cmsExamService.deleteExam(COURSE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No exam found");
    }

    @Test
    void learnerCannotUpdateExam() {
        setUpAuth("learner-uid", OTHER_USER_ID, UserRole.LEARNER);
        givenCourseOwnedBy(OWNER_ID);

        assertThatThrownBy(() -> cmsExamService.updateExam(COURSE_ID, validRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Not the course owner");
    }

    @Test
    void learnerCannotDeleteExam() {
        setUpAuth("learner-uid", OTHER_USER_ID, UserRole.LEARNER);
        givenCourseOwnedBy(OWNER_ID);

        assertThatThrownBy(() -> cmsExamService.deleteExam(COURSE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Not the course owner");
    }

    @Test
    void getAfterDeleteReturns404() {
        setUpAuth("owner-uid", OWNER_ID, UserRole.TEACHER);
        givenCourseOwnedBy(OWNER_ID);
        given(examRepository.findByCourseId(COURSE_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> cmsExamService.getExam(COURSE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No exam found");
    }

    // ── P1: CMS exam read must enforce course ownership (answer key disclosure) ──

    @Test
    void ownerCanReadExamWithCorrectAnswerFlags() {
        setUpAuth("owner-uid", OWNER_ID, UserRole.TEACHER);
        givenCourseOwnedBy(OWNER_ID);
        givenExamWithOneQuestionTwoChoices();

        ExamAdminDto result = cmsExamService.getExam(COURSE_ID);

        // CMS view is owner/admin only, so it intentionally exposes isCorrect.
        assertThat(result.questions()).hasSize(1);
        assertThat(result.questions().get(0).choices())
                .anyMatch(ExamAdminDto.ChoiceDto::correct);
    }

    @Test
    void nonOwnerTeacherCannotReadExam() {
        setUpAuth("other-uid", OTHER_USER_ID, UserRole.TEACHER);
        givenCourseOwnedBy(OWNER_ID);

        assertThatThrownBy(() -> cmsExamService.getExam(COURSE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Not the course owner");
    }

    @Test
    void adminCanReadAnyExam() {
        setUpAuth("admin-uid", OTHER_USER_ID, UserRole.ADMIN);
        givenCourseOwnedBy(OWNER_ID);
        givenExamWithOneQuestionTwoChoices();

        ExamAdminDto result = cmsExamService.getExam(COURSE_ID);

        assertThat(result.questions()).hasSize(1);
    }

    @Test
    void learnerCannotReadExam() {
        setUpAuth("learner-uid", OTHER_USER_ID, UserRole.LEARNER);
        givenCourseOwnedBy(OWNER_ID);

        assertThatThrownBy(() -> cmsExamService.getExam(COURSE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Not the course owner");
    }

    @Test
    void unauthorizedGroupAdminCannotReadExam() {
        setUpAuth("ga-uid", OTHER_USER_ID, UserRole.GROUP_ADMIN);
        givenCourseOwnedBy(OWNER_ID);
        given(groupMemberRepository.existsMemberManagedBy(OTHER_USER_ID, OWNER_ID)).willReturn(false);

        assertThatThrownBy(() -> cmsExamService.getExam(COURSE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Not the course owner");
    }

    @Test
    void authorizedGroupAdminCanReadExamForManagedAuthor() {
        setUpAuth("ga-uid", OTHER_USER_ID, UserRole.GROUP_ADMIN);
        givenCourseOwnedBy(OWNER_ID);
        given(groupMemberRepository.existsMemberManagedBy(OTHER_USER_ID, OWNER_ID)).willReturn(true);
        givenExamWithOneQuestionTwoChoices();

        ExamAdminDto result = cmsExamService.getExam(COURSE_ID);

        assertThat(result.questions()).hasSize(1);
    }

    // ── helpers ──

    private void givenExamWithOneQuestionTwoChoices() {
        Exam exam = new Exam(COURSE_ID, "Exam", 80, 20);
        ReflectionTestUtils.setField(exam, "id", EXAM_ID);
        given(examRepository.findByCourseId(COURSE_ID)).willReturn(Optional.of(exam));

        ExamQuestion q1 = new ExamQuestion(EXAM_ID, "Q?", 1);
        ReflectionTestUtils.setField(q1, "id", Q1_ID);
        given(questionRepository.findAllByExamIdOrderByOrderIndexAsc(EXAM_ID)).willReturn(List.of(q1));

        ExamChoice correct = new ExamChoice(Q1_ID, "A", true);
        ExamChoice wrong = new ExamChoice(Q1_ID, "B", false);
        ReflectionTestUtils.setField(correct, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(wrong, "id", UUID.randomUUID());
        given(choiceRepository.findAllByQuestionIdIn(List.of(Q1_ID)))
                .willReturn(List.of(correct, wrong));
    }

    private void setUpAuth(String firebaseUid, UUID userId, UserRole role) {
        SecurityContextHolder.getContext().setAuthentication(
                new FirebaseAuthentication(firebaseUid, "user@edulife.test", null)
        );
        User user = mock(User.class);
        // getId() is unused on the ADMIN short-circuit path, so keep it lenient.
        lenient().when(user.getId()).thenReturn(userId);
        given(user.getRole()).willReturn(role);
        given(userRepository.findByFirebaseUid(firebaseUid)).willReturn(Optional.of(user));
    }

    private void givenCourseOwnedBy(UUID ownerId) {
        Course course = mock(Course.class);
        // getCreatedByUserId() is unused on the ADMIN short-circuit path, so keep it lenient.
        lenient().when(course.getCreatedByUserId()).thenReturn(ownerId);
        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.of(course));
    }

    private void givenExistingExamEntity() {
        Exam exam = new Exam(COURSE_ID, "Old Exam", 80, 20);
        ReflectionTestUtils.setField(exam, "id", EXAM_ID);
        given(examRepository.findByCourseId(COURSE_ID)).willReturn(Optional.of(exam));
    }

    private void givenExistingExamWithQuestions() {
        givenExistingExamEntity();
        ExamQuestion q1 = new ExamQuestion(EXAM_ID, "Old Q?", 1);
        ReflectionTestUtils.setField(q1, "id", Q1_ID);
        given(questionRepository.findAllByExamIdOrderByOrderIndexAsc(EXAM_ID)).willReturn(List.of(q1));
    }

    private void givenSaveReturnsInput() {
        given(examRepository.save(any(Exam.class))).willAnswer(i -> i.getArgument(0));
        given(questionRepository.save(any(ExamQuestion.class))).willAnswer(i -> {
            ExamQuestion q = i.getArgument(0);
            ReflectionTestUtils.setField(q, "id", UUID.randomUUID());
            return q;
        });
        given(choiceRepository.save(any(ExamChoice.class))).willAnswer(i -> {
            ExamChoice c = i.getArgument(0);
            ReflectionTestUtils.setField(c, "id", UUID.randomUUID());
            return c;
        });
    }

    private CreateExamRequest validRequest() {
        return new CreateExamRequest("Updated Exam", 80, 30,
                List.of(new CreateExamRequest.QuestionRequest("What is X?", 1,
                        List.of(new CreateExamRequest.ChoiceRequest("A", true),
                                new CreateExamRequest.ChoiceRequest("B", false)))));
    }
}

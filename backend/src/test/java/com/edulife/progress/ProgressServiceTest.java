package com.edulife.progress;

import com.edulife.courses.entity.CourseSection;
import com.edulife.courses.entity.Lesson;
import com.edulife.courses.repository.CourseSectionRepository;
import com.edulife.courses.repository.LessonRepository;
import com.edulife.enrollments.model.EnrollmentStatus;
import com.edulife.enrollments.repository.EnrollmentRepository;
import com.edulife.gamification.service.GamificationService;
import com.edulife.progress.dto.CourseProgressDto;
import com.edulife.progress.entity.CourseProgress;
import com.edulife.progress.entity.LessonProgress;
import com.edulife.progress.repository.CourseProgressRepository;
import com.edulife.progress.repository.LessonProgressRepository;
import com.edulife.progress.service.ProgressService;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    private static final UUID USER_ID    = UUID.fromString("bbbbbbbb-0000-0000-0000-bbbbbbbbbbbb");
    private static final UUID COURSE_ID  = UUID.fromString("aaaaaaaa-0000-0000-0000-aaaaaaaaaaaa");
    private static final UUID SECTION_ID = UUID.fromString("cccccccc-0000-0000-0000-cccccccccccc");
    private static final UUID LESSON_1   = UUID.fromString("11111111-0000-0000-0000-111111111111");
    private static final UUID LESSON_2   = UUID.fromString("22222222-0000-0000-0000-222222222222");
    private static final UUID LESSON_3   = UUID.fromString("33333333-0000-0000-0000-333333333333");

    @Mock private LessonProgressRepository lessonProgressRepository;
    @Mock private CourseProgressRepository courseProgressRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private CourseSectionRepository sectionRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private GamificationService gamificationService;

    @InjectMocks
    private ProgressService progressService;

    private User mockUser;

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContextHolder.getContext()
                .setAuthentication(new FirebaseAuthentication("firebase-uid-123", "student@test.com", null));
        mockUser = mock(User.class);
        given(mockUser.getId()).willReturn(USER_ID);
        given(userRepository.findByFirebaseUid("firebase-uid-123")).willReturn(Optional.of(mockUser));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ── markLessonComplete ────────────────────────────────────────────────────

    @Test
    void markLessonComplete_firstCallSavesProgressRecord() {
        Lesson lesson = mockLesson(LESSON_1, false);
        given(lessonRepository.findByIdAndCourseId(LESSON_1, COURSE_ID)).willReturn(Optional.of(lesson));
        given(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(USER_ID, COURSE_ID, EnrollmentStatus.ACTIVE)).willReturn(true);
        given(lessonProgressRepository.existsByUserIdAndLessonId(USER_ID, LESSON_1)).willReturn(false);
        given(lessonRepository.countByCourseId(COURSE_ID)).willReturn(3L);
        given(lessonProgressRepository.countByUserIdAndCourseId(USER_ID, COURSE_ID)).willReturn(1L);
        given(courseProgressRepository.findByUserIdAndCourseId(USER_ID, COURSE_ID)).willReturn(Optional.empty());

        progressService.markLessonComplete(COURSE_ID, LESSON_1);

        verify(lessonProgressRepository).save(any(LessonProgress.class));
    }

    @Test
    void markLessonComplete_secondCallIsIdempotentAndSkipsSave() {
        Lesson lesson = mockLesson(LESSON_1, false);
        given(lessonRepository.findByIdAndCourseId(LESSON_1, COURSE_ID)).willReturn(Optional.of(lesson));
        given(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(USER_ID, COURSE_ID, EnrollmentStatus.ACTIVE)).willReturn(true);
        given(lessonProgressRepository.existsByUserIdAndLessonId(USER_ID, LESSON_1)).willReturn(true);

        progressService.markLessonComplete(COURSE_ID, LESSON_1);

        verify(lessonProgressRepository, never()).save(any());
    }

    @Test
    void markLessonComplete_throwsForbiddenWhenNotEnrolled() {
        Lesson lesson = mockLesson(LESSON_1, false);
        given(lessonRepository.findByIdAndCourseId(LESSON_1, COURSE_ID)).willReturn(Optional.of(lesson));
        given(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(USER_ID, COURSE_ID, EnrollmentStatus.ACTIVE)).willReturn(false);

        assertThatThrownBy(() -> progressService.markLessonComplete(COURSE_ID, LESSON_1))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(403);
    }

    @Test
    void markLessonComplete_syncsCourseProgressAfterFirstCompletion() {
        Lesson lesson = mockLesson(LESSON_1, false);
        given(lessonRepository.findByIdAndCourseId(LESSON_1, COURSE_ID)).willReturn(Optional.of(lesson));
        given(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(USER_ID, COURSE_ID, EnrollmentStatus.ACTIVE)).willReturn(true);
        given(lessonProgressRepository.existsByUserIdAndLessonId(USER_ID, LESSON_1)).willReturn(false);
        given(lessonRepository.countByCourseId(COURSE_ID)).willReturn(3L);
        given(lessonProgressRepository.countByUserIdAndCourseId(USER_ID, COURSE_ID)).willReturn(1L);
        given(courseProgressRepository.findByUserIdAndCourseId(USER_ID, COURSE_ID)).willReturn(Optional.empty());

        progressService.markLessonComplete(COURSE_ID, LESSON_1);

        verify(courseProgressRepository).save(any(CourseProgress.class));
    }

    // ── getCourseProgress ─────────────────────────────────────────────────────

    @Test
    void getCourseProgress_throwsForbiddenWhenNotEnrolled() {
        given(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(USER_ID, COURSE_ID, EnrollmentStatus.ACTIVE)).willReturn(false);

        assertThatThrownBy(() -> progressService.getCourseProgress(COURSE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(403);
    }

    @Test
    void getCourseProgress_returnsCorrectLessonCompletionFlags() {
        Instant completedAt = Instant.parse("2026-05-25T10:00:00Z");
        given(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(USER_ID, COURSE_ID, EnrollmentStatus.ACTIVE)).willReturn(true);

        CourseSection section = mockSection(SECTION_ID, "Algebra Basics", 1);
        given(sectionRepository.findAllByCourseIdOrderByDisplayOrderAsc(COURSE_ID)).willReturn(List.of(section));

        Lesson l1 = mockLesson(LESSON_1, false);
        Lesson l2 = mockLesson(LESSON_2, false);
        given(lessonRepository.findAllByCourseSectionIdOrderByDisplayOrderAsc(SECTION_ID)).willReturn(List.of(l1, l2));

        LessonProgress lp = mock(LessonProgress.class);
        given(lp.getLessonId()).willReturn(LESSON_1);
        given(lp.getCompletedAt()).willReturn(completedAt);
        given(lessonProgressRepository.findAllByUserIdAndCourseId(USER_ID, COURSE_ID)).willReturn(List.of(lp));

        CourseProgressDto result = progressService.getCourseProgress(COURSE_ID);

        assertThat(result.courseId()).isEqualTo(COURSE_ID);
        assertThat(result.completedLessons()).isEqualTo(1);
        assertThat(result.totalLessons()).isEqualTo(2);
        assertThat(result.percentComplete()).isEqualTo(50.0);
        assertThat(result.sections()).hasSize(1);

        CourseProgressDto.SectionProgressDto s = result.sections().get(0);
        assertThat(s.sectionId()).isEqualTo(SECTION_ID);
        assertThat(s.title()).isEqualTo("Algebra Basics");
        assertThat(s.lessons()).hasSize(2);

        CourseProgressDto.LessonProgressDto done = s.lessons().get(0);
        assertThat(done.lessonId()).isEqualTo(LESSON_1);
        assertThat(done.completed()).isTrue();
        assertThat(done.completedAt()).isEqualTo(completedAt);

        CourseProgressDto.LessonProgressDto todo = s.lessons().get(1);
        assertThat(todo.lessonId()).isEqualTo(LESSON_2);
        assertThat(todo.completed()).isFalse();
        assertThat(todo.completedAt()).isNull();
    }

    @Test
    void getCourseProgress_returns100PercentWhenAllLessonsComplete() {
        given(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(USER_ID, COURSE_ID, EnrollmentStatus.ACTIVE)).willReturn(true);

        CourseSection section = mockSection(SECTION_ID, "Section", 1);
        given(sectionRepository.findAllByCourseIdOrderByDisplayOrderAsc(COURSE_ID)).willReturn(List.of(section));

        Lesson l1 = mockLesson(LESSON_1, false);
        Lesson l2 = mockLesson(LESSON_2, false);
        Lesson l3 = mockLesson(LESSON_3, false);
        given(lessonRepository.findAllByCourseSectionIdOrderByDisplayOrderAsc(SECTION_ID)).willReturn(List.of(l1, l2, l3));

        List<LessonProgress> completedLessons = List.of(
                mockLessonProgress(LESSON_1),
                mockLessonProgress(LESSON_2),
                mockLessonProgress(LESSON_3)
        );
        given(lessonProgressRepository.findAllByUserIdAndCourseId(USER_ID, COURSE_ID))
                .willReturn(completedLessons);

        CourseProgressDto result = progressService.getCourseProgress(COURSE_ID);

        assertThat(result.completedLessons()).isEqualTo(3);
        assertThat(result.totalLessons()).isEqualTo(3);
        assertThat(result.percentComplete()).isEqualTo(100.0);
        assertThat(result.sections().get(0).lessons())
                .allMatch(CourseProgressDto.LessonProgressDto::completed);
    }

    @Test
    void getCourseProgress_recomputesPercentAccuratelyForPartialCompletion() {
        given(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(USER_ID, COURSE_ID, EnrollmentStatus.ACTIVE)).willReturn(true);

        CourseSection section = mockSection(SECTION_ID, "Section", 1);
        given(sectionRepository.findAllByCourseIdOrderByDisplayOrderAsc(COURSE_ID)).willReturn(List.of(section));

        Lesson l1 = mockLesson(LESSON_1, false);
        Lesson l2 = mockLesson(LESSON_2, false);
        Lesson l3 = mockLesson(LESSON_3, false);
        given(lessonRepository.findAllByCourseSectionIdOrderByDisplayOrderAsc(SECTION_ID)).willReturn(List.of(l1, l2, l3));

        // 1 of 3 completed → 33.3%
        List<LessonProgress> completedLessons = List.of(mockLessonProgress(LESSON_1));
        given(lessonProgressRepository.findAllByUserIdAndCourseId(USER_ID, COURSE_ID))
                .willReturn(completedLessons);

        CourseProgressDto result = progressService.getCourseProgress(COURSE_ID);

        assertThat(result.completedLessons()).isEqualTo(1);
        assertThat(result.totalLessons()).isEqualTo(3);
        assertThat(result.percentComplete()).isEqualTo(33.3);
    }

    @Test
    void getCourseProgress_returnsZeroPercentWhenCourseHasNoLessons() {
        given(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(USER_ID, COURSE_ID, EnrollmentStatus.ACTIVE)).willReturn(true);
        given(sectionRepository.findAllByCourseIdOrderByDisplayOrderAsc(COURSE_ID)).willReturn(List.of());
        given(lessonProgressRepository.findAllByUserIdAndCourseId(USER_ID, COURSE_ID)).willReturn(List.of());

        CourseProgressDto result = progressService.getCourseProgress(COURSE_ID);

        assertThat(result.totalLessons()).isEqualTo(0);
        assertThat(result.completedLessons()).isEqualTo(0);
        assertThat(result.percentComplete()).isEqualTo(0.0);
        assertThat(result.sections()).isEmpty();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Lesson mockLesson(UUID id, boolean preview) {
        Lesson l = mock(Lesson.class);
        lenient().when(l.getId()).thenReturn(id);
        lenient().when(l.getTitle()).thenReturn("Lesson " + id.toString().substring(0, 8));
        lenient().when(l.getLessonType()).thenReturn("VIDEO");
        lenient().when(l.getEstimatedDurationMinutes()).thenReturn(10);
        lenient().when(l.getDisplayOrder()).thenReturn(1);
        lenient().when(l.isPreview()).thenReturn(preview);
        return l;
    }

    private CourseSection mockSection(UUID id, String title, int order) {
        CourseSection s = mock(CourseSection.class);
        given(s.getId()).willReturn(id);
        given(s.getTitle()).willReturn(title);
        given(s.getDisplayOrder()).willReturn(order);
        return s;
    }

    private LessonProgress mockLessonProgress(UUID lessonId) {
        LessonProgress lp = mock(LessonProgress.class);
        given(lp.getLessonId()).willReturn(lessonId);
        given(lp.getCompletedAt()).willReturn(Instant.parse("2026-05-25T10:00:00Z"));
        return lp;
    }
}

package com.edulife.analytics;

import com.edulife.analytics.dto.StudentAnalyticsSummaryDto;
import com.edulife.analytics.dto.TeacherAnalyticsDto;
import com.edulife.analytics.service.AnalyticsService;
import com.edulife.certificates.repository.CertificateRepository;
import com.edulife.courses.entity.Course;
import com.edulife.courses.model.CourseStatus;
import com.edulife.courses.repository.CourseRepository;
import com.edulife.enrollments.model.EnrollmentStatus;
import com.edulife.enrollments.repository.EnrollmentRepository;
import com.edulife.exams.repository.ExamAttemptRepository;
import com.edulife.exams.repository.ExamRepository;
import com.edulife.progress.repository.CourseProgressRepository;
import com.edulife.progress.repository.LessonProgressRepository;
import com.edulife.security.FirebaseAuthentication;
import com.edulife.users.entity.User;
import com.edulife.users.repository.UserRepository;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests focused on ownership/scoping: the service must derive scope from the resolved user
 * id only, and must never fall back to a missing user.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    private static final String FIREBASE_UID = "firebase-uid-analytics";
    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_TEACHER_COURSE = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID OWNED_COURSE = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock private UserRepository userRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private LessonProgressRepository lessonProgressRepository;
    @Mock private CourseProgressRepository courseProgressRepository;
    @Mock private ExamRepository examRepository;
    @Mock private ExamAttemptRepository examAttemptRepository;
    @Mock private CertificateRepository certificateRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContextHolder.getContext()
                .setAuthentication(new FirebaseAuthentication(FIREBASE_UID, "user@test.com", null));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void givenResolvedUser() {
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(OWNER_ID);
        given(userRepository.findByFirebaseUid(FIREBASE_UID)).willReturn(Optional.of(user));
    }

    // ── student summary scoping ─────────────────────────────────────────────────

    @Test
    void studentSummary_usesResolvedUserIdForEveryCount() {
        givenResolvedUser();
        given(enrollmentRepository.countByUserIdAndStatus(OWNER_ID, EnrollmentStatus.ACTIVE)).willReturn(2L);
        given(lessonProgressRepository.countByUserId(OWNER_ID)).willReturn(7L);
        given(examAttemptRepository.countByUserId(OWNER_ID)).willReturn(3L);
        given(examAttemptRepository.countByUserIdAndPassedTrue(OWNER_ID)).willReturn(1L);
        given(certificateRepository.countByUserId(OWNER_ID)).willReturn(1L);
        given(examAttemptRepository.averageScoreByUserId(OWNER_ID)).willReturn(82.4);
        given(examAttemptRepository.maxScoreByUserId(OWNER_ID)).willReturn(96);

        StudentAnalyticsSummaryDto dto = analyticsService.getMyStudentSummary();

        assertThat(dto.activeEnrollments()).isEqualTo(2L);
        assertThat(dto.lessonsCompleted()).isEqualTo(7L);
        assertThat(dto.examAttempts()).isEqualTo(3L);
        assertThat(dto.examsPassed()).isEqualTo(1L);
        assertThat(dto.certificatesEarned()).isEqualTo(1L);
        assertThat(dto.averageExamScore()).isEqualTo(82);
        assertThat(dto.bestExamScore()).isEqualTo(96);
        // Every count was keyed by the resolved id — no other user id is ever used.
        verify(enrollmentRepository).countByUserIdAndStatus(OWNER_ID, EnrollmentStatus.ACTIVE);
        verify(certificateRepository).countByUserId(OWNER_ID);
    }

    @Test
    void studentSummary_handlesNullScoreAggregatesForLearnerWithNoAttempts() {
        givenResolvedUser();
        given(enrollmentRepository.countByUserIdAndStatus(OWNER_ID, EnrollmentStatus.ACTIVE)).willReturn(0L);
        given(lessonProgressRepository.countByUserId(OWNER_ID)).willReturn(0L);
        given(examAttemptRepository.countByUserId(OWNER_ID)).willReturn(0L);
        given(examAttemptRepository.countByUserIdAndPassedTrue(OWNER_ID)).willReturn(0L);
        given(certificateRepository.countByUserId(OWNER_ID)).willReturn(0L);
        given(examAttemptRepository.averageScoreByUserId(OWNER_ID)).willReturn(null);
        given(examAttemptRepository.maxScoreByUserId(OWNER_ID)).willReturn(null);

        StudentAnalyticsSummaryDto dto = analyticsService.getMyStudentSummary();

        assertThat(dto.averageExamScore()).isZero();
        assertThat(dto.bestExamScore()).isZero();
    }

    // ── teacher analytics ownership ─────────────────────────────────────────────

    @Test
    void teacherAnalytics_onlyQueriesCoursesOwnedByResolvedTeacher() {
        givenResolvedUser();

        Course owned = mock(Course.class);
        lenient().when(owned.getId()).thenReturn(OWNED_COURSE);
        lenient().when(owned.getTitle()).thenReturn("Owned Course");
        lenient().when(owned.getStatus()).thenReturn(CourseStatus.PUBLISHED);

        // Ownership is enforced via this scoped query; the service must call it with OWNER_ID.
        given(courseRepository.findAllByCreatedByUserId(OWNER_ID)).willReturn(List.of(owned));
        given(enrollmentRepository.countByCourseIdAndStatus(OWNED_COURSE, EnrollmentStatus.ACTIVE)).willReturn(4L);
        given(courseProgressRepository.countByCourseId(OWNED_COURSE)).willReturn(4L);
        given(courseProgressRepository.countCompletedByCourseId(OWNED_COURSE)).willReturn(1L);
        given(examRepository.findByCourseId(OWNED_COURSE)).willReturn(Optional.empty());
        given(certificateRepository.countByCourseId(OWNED_COURSE)).willReturn(1L);

        TeacherAnalyticsDto dto = analyticsService.getMyTeacherAnalytics();

        assertThat(dto.totalCourses()).isEqualTo(1);
        assertThat(dto.courses().get(0).courseId()).isEqualTo(OWNED_COURSE);
        // 1 of 4 learners completed -> 25.0%
        assertThat(dto.courses().get(0).completionRatePercent()).isEqualTo(25.0);
        // No exam -> attempts/passed default to zero, pass rate 0.0 (no divide-by-zero).
        assertThat(dto.courses().get(0).examAttempts()).isEqualTo(0L);
        assertThat(dto.courses().get(0).passRatePercent()).isEqualTo(0.0);

        // Scope query was called with the resolved id; the other teacher's course is never touched.
        verify(courseRepository).findAllByCreatedByUserId(OWNER_ID);
        verify(enrollmentRepository).countByCourseIdAndStatus(OWNED_COURSE, EnrollmentStatus.ACTIVE);
    }

    @Test
    void teacherAnalytics_emptyWhenTeacherOwnsNoCourses() {
        givenResolvedUser();
        given(courseRepository.findAllByCreatedByUserId(OWNER_ID)).willReturn(List.of());

        TeacherAnalyticsDto dto = analyticsService.getMyTeacherAnalytics();

        assertThat(dto.totalCourses()).isZero();
        assertThat(dto.courses()).isEmpty();
    }

    // ── missing user ────────────────────────────────────────────────────────────

    @Test
    void throwsUnauthorizedWhenUserNotSynced() {
        given(userRepository.findByFirebaseUid(FIREBASE_UID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> analyticsService.getMyStudentSummary())
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(401);
    }
}

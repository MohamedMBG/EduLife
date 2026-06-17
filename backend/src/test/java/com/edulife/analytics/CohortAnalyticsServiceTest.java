package com.edulife.analytics;

import com.edulife.analytics.dto.GroupCohortAnalyticsDto;
import com.edulife.analytics.dto.StudentProgressTrendDto;
import com.edulife.analytics.dto.TeacherCohortAnalyticsDto;
import com.edulife.analytics.repository.CohortAnalyticsRepository;
import com.edulife.analytics.repository.FunnelProjection;
import com.edulife.analytics.repository.MonthCountProjection;
import com.edulife.analytics.service.CohortAnalyticsService;
import com.edulife.courses.entity.Course;
import com.edulife.courses.repository.CourseRepository;
import com.edulife.groups.entity.Group;
import com.edulife.groups.entity.GroupCourse;
import com.edulife.groups.entity.GroupMember;
import com.edulife.groups.repository.GroupCourseRepository;
import com.edulife.groups.repository.GroupMemberRepository;
import com.edulife.groups.repository.GroupRepository;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Ownership/scoping unit tests for Phase C cohort analytics. Focus: scope is derived from the
 * resolved user, and group access is denied to non-owners.
 */
@ExtendWith(MockitoExtension.class)
class CohortAnalyticsServiceTest {

    private static final String FIREBASE_UID = "firebase-uid-cohort";
    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID COURSE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID GROUP_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID MEMBER_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Mock private CohortAnalyticsRepository cohortRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private GroupRepository groupRepository;
    @Mock private GroupCourseRepository groupCourseRepository;
    @Mock private GroupMemberRepository groupMemberRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private CohortAnalyticsService service;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext()
                .setAuthentication(new FirebaseAuthentication(FIREBASE_UID, "user@test.com", null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User resolvedUser(UserRole role) {
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(OWNER_ID);
        lenient().when(user.getRole()).thenReturn(role);
        given(userRepository.findByFirebaseUid(FIREBASE_UID)).willReturn(Optional.of(user));
        return user;
    }

    private FunnelProjection funnel(long enrolled, long started, long completed, long passed, long certified) {
        FunnelProjection p = mock(FunnelProjection.class);
        lenient().when(p.getEnrolled()).thenReturn(enrolled);
        lenient().when(p.getStarted()).thenReturn(started);
        lenient().when(p.getCompleted()).thenReturn(completed);
        lenient().when(p.getPassed()).thenReturn(passed);
        lenient().when(p.getCertified()).thenReturn(certified);
        return p;
    }

    private MonthCountProjection month(String m, long total) {
        MonthCountProjection p = mock(MonthCountProjection.class);
        lenient().when(p.getMonth()).thenReturn(m);
        lenient().when(p.getTotal()).thenReturn(total);
        return p;
    }

    // ── student trend ────────────────────────────────────────────────────────────

    @Test
    void studentTrend_scopedToResolvedUserAndSumsTotal() {
        resolvedUser(UserRole.LEARNER);
        // Build projection stubs first; nesting them inside given(...).willReturn(...) trips Mockito.
        MonthCountProjection m1 = month("2026-05", 3);
        MonthCountProjection m2 = month("2026-06", 4);
        given(cohortRepository.lessonTrendByUser(OWNER_ID)).willReturn(List.of(m1, m2));

        StudentProgressTrendDto dto = service.getMyProgressTrend();

        assertThat(dto.totalLessons()).isEqualTo(7);
        assertThat(dto.lessonsByMonth()).hasSize(2);
        verify(cohortRepository).lessonTrendByUser(OWNER_ID);
    }

    // ── teacher cohorts ──────────────────────────────────────────────────────────

    @Test
    void teacherCohorts_emptyWhenNoOwnedCourses_skipsDbAggregates() {
        resolvedUser(UserRole.TEACHER);
        given(courseRepository.findAllByCreatedByUserId(OWNER_ID)).willReturn(List.of());

        TeacherCohortAnalyticsDto dto = service.getMyTeacherCohorts();

        assertThat(dto.courseCount()).isZero();
        assertThat(dto.funnel().enrolled()).isZero();
        assertThat(dto.enrollmentCohorts()).isEmpty();
        // No course scope -> no funnel query should run.
        verify(cohortRepository, never()).funnelByCourseIds(org.mockito.ArgumentMatchers.anyCollection());
    }

    @Test
    void teacherCohorts_scopesFunnelToOwnedCourseIds() {
        resolvedUser(UserRole.TEACHER);
        Course owned = mock(Course.class);
        lenient().when(owned.getId()).thenReturn(COURSE_ID);
        given(courseRepository.findAllByCreatedByUserId(OWNER_ID)).willReturn(List.of(owned));
        FunnelProjection f = funnel(10, 8, 5, 4, 3);
        MonthCountProjection mc = month("2026-06", 10);
        given(cohortRepository.funnelByCourseIds(List.of(COURSE_ID))).willReturn(f);
        given(cohortRepository.enrollmentCohortsByCourseIds(List.of(COURSE_ID))).willReturn(List.of(mc));

        TeacherCohortAnalyticsDto dto = service.getMyTeacherCohorts();

        assertThat(dto.courseCount()).isEqualTo(1);
        assertThat(dto.funnel().enrolled()).isEqualTo(10);
        assertThat(dto.funnel().certified()).isEqualTo(3);
        assertThat(dto.enrollmentCohorts()).hasSize(1);
        verify(cohortRepository).funnelByCourseIds(List.of(COURSE_ID));
    }

    // ── group cohorts (ownership) ─────────────────────────────────────────────────

    @Test
    void groupCohorts_forbiddenForNonOwnerNonAdmin() {
        resolvedUser(UserRole.GROUP_ADMIN);
        Group group = mock(Group.class);
        given(group.getCreatedBy()).willReturn(OTHER_ID); // owned by someone else
        given(groupRepository.findById(GROUP_ID)).willReturn(Optional.of(group));

        assertThatThrownBy(() -> service.getGroupCohorts(GROUP_ID))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(403);

        // No cohort query may run once ownership fails.
        verify(cohortRepository, never())
                .funnelByGroup(org.mockito.ArgumentMatchers.anyCollection(), org.mockito.ArgumentMatchers.anyCollection());
    }

    @Test
    void groupCohorts_allowedForOwnerAndScopedToGroupCoursesAndMembers() {
        resolvedUser(UserRole.GROUP_ADMIN);
        Group group = mock(Group.class);
        given(group.getCreatedBy()).willReturn(OWNER_ID);
        given(group.getId()).willReturn(GROUP_ID);
        given(group.getName()).willReturn("Cohort A");
        given(groupRepository.findById(GROUP_ID)).willReturn(Optional.of(group));

        GroupCourse gc = mock(GroupCourse.class);
        given(gc.getCourseId()).willReturn(COURSE_ID);
        given(groupCourseRepository.findAllByGroupId(GROUP_ID)).willReturn(List.of(gc));
        GroupMember gm = mock(GroupMember.class);
        given(gm.getUserId()).willReturn(MEMBER_ID);
        given(groupMemberRepository.findAllByGroupId(GROUP_ID)).willReturn(List.of(gm));

        FunnelProjection f = funnel(5, 4, 2, 2, 1);
        given(cohortRepository.funnelByGroup(List.of(COURSE_ID), List.of(MEMBER_ID))).willReturn(f);

        GroupCohortAnalyticsDto dto = service.getGroupCohorts(GROUP_ID);

        assertThat(dto.groupId()).isEqualTo(GROUP_ID);
        assertThat(dto.memberCount()).isEqualTo(1);
        assertThat(dto.courseCount()).isEqualTo(1);
        assertThat(dto.funnel().enrolled()).isEqualTo(5);
        verify(cohortRepository).funnelByGroup(List.of(COURSE_ID), List.of(MEMBER_ID));
    }

    @Test
    void groupCohorts_platformAdminMayReadAnyGroup() {
        resolvedUser(UserRole.ADMIN);
        Group group = mock(Group.class);
        given(group.getCreatedBy()).willReturn(OTHER_ID); // not the admin
        given(group.getId()).willReturn(GROUP_ID);
        given(group.getName()).willReturn("Cohort B");
        given(groupRepository.findById(GROUP_ID)).willReturn(Optional.of(group));
        given(groupCourseRepository.findAllByGroupId(GROUP_ID)).willReturn(List.of());
        given(groupMemberRepository.findAllByGroupId(GROUP_ID)).willReturn(List.of());

        GroupCohortAnalyticsDto dto = service.getGroupCohorts(GROUP_ID);

        // Empty group -> zero funnel, no funnel query.
        assertThat(dto.funnel().enrolled()).isZero();
        verify(cohortRepository, never())
                .funnelByGroup(org.mockito.ArgumentMatchers.anyCollection(), org.mockito.ArgumentMatchers.anyCollection());
    }

    // ── auth ──────────────────────────────────────────────────────────────────────

    @Test
    void throwsUnauthorizedWhenUserNotSynced() {
        given(userRepository.findByFirebaseUid(FIREBASE_UID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyProgressTrend())
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(401);
    }
}

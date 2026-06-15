package com.edulife.analytics.service;

import com.edulife.analytics.dto.FunnelDto;
import com.edulife.analytics.dto.GroupCohortAnalyticsDto;
import com.edulife.analytics.dto.MonthCountDto;
import com.edulife.analytics.dto.PlatformCohortAnalyticsDto;
import com.edulife.analytics.dto.StudentProgressTrendDto;
import com.edulife.analytics.dto.TeacherCohortAnalyticsDto;
import com.edulife.analytics.repository.CohortAnalyticsRepository;
import com.edulife.analytics.repository.FunnelProjection;
import com.edulife.analytics.repository.MonthCountProjection;
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
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Read-only Phase C cohort/progress analytics. Every method resolves the caller server-side and
 * derives its scope (own id, owned courses, owned group) from trusted data — never from client
 * input. No writes; all methods are {@code @Transactional(readOnly = true)}.
 */
@Service
public class CohortAnalyticsService {

    private final CohortAnalyticsRepository cohortRepository;
    private final CourseRepository courseRepository;
    private final GroupRepository groupRepository;
    private final GroupCourseRepository groupCourseRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    public CohortAnalyticsService(
            CohortAnalyticsRepository cohortRepository,
            CourseRepository courseRepository,
            GroupRepository groupRepository,
            GroupCourseRepository groupCourseRepository,
            GroupMemberRepository groupMemberRepository,
            UserRepository userRepository
    ) {
        this.cohortRepository = cohortRepository;
        this.courseRepository = courseRepository;
        this.groupRepository = groupRepository;
        this.groupCourseRepository = groupCourseRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
    }

    /** Student's own lessons-completed trend by month. Scoped strictly to the resolved user id. */
    @Transactional(readOnly = true)
    public StudentProgressTrendDto getMyProgressTrend() {
        UUID userId = resolveCurrentUser().getId();
        List<MonthCountDto> byMonth = toMonthCounts(cohortRepository.lessonTrendByUser(userId));
        long total = byMonth.stream().mapToLong(MonthCountDto::count).sum();
        return new StudentProgressTrendDto(total, byMonth);
    }

    /**
     * Teacher cohort analytics across the caller's own courses. Ownership is enforced by scoping
     * to courses authored by the resolved teacher; no courseId/teacherId is accepted from the
     * client. An empty owned-course set returns an empty funnel without touching the DB.
     */
    @Transactional(readOnly = true)
    public TeacherCohortAnalyticsDto getMyTeacherCohorts() {
        UUID teacherId = resolveCurrentUser().getId();

        List<UUID> courseIds = courseRepository.findAllByCreatedByUserId(teacherId).stream()
                .map(Course::getId)
                .toList();

        if (courseIds.isEmpty()) {
            return new TeacherCohortAnalyticsDto(0, FunnelDto.empty(), List.of());
        }

        FunnelDto funnel = toFunnel(cohortRepository.funnelByCourseIds(courseIds));
        List<MonthCountDto> cohorts = toMonthCounts(cohortRepository.enrollmentCohortsByCourseIds(courseIds));
        return new TeacherCohortAnalyticsDto(courseIds.size(), funnel, cohorts);
    }

    /**
     * Group cohort analytics. Ownership: the caller must be the group creator, or a platform admin.
     * Scope is enrollments where the course is attached to the group AND the learner is a group
     * member, so a group admin only ever sees their own group's cohort.
     */
    @Transactional(readOnly = true)
    public GroupCohortAnalyticsDto getGroupCohorts(UUID groupId) {
        User caller = resolveCurrentUser();

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        // Ownership check mirrors GroupService.loadGroupForManagement: creator or platform admin.
        boolean isPlatformAdmin = caller.getRole() == UserRole.ADMIN;
        boolean isCreator = group.getCreatedBy().equals(caller.getId());
        if (!isPlatformAdmin && !isCreator) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not the group owner");
        }

        List<UUID> courseIds = groupCourseRepository.findAllByGroupId(groupId).stream()
                .map(GroupCourse::getCourseId)
                .toList();
        List<UUID> memberIds = groupMemberRepository.findAllByGroupId(groupId).stream()
                .map(GroupMember::getUserId)
                .toList();

        // Empty course or member set means there is no cohort to measure yet.
        FunnelDto funnel = (courseIds.isEmpty() || memberIds.isEmpty())
                ? FunnelDto.empty()
                : toFunnel(cohortRepository.funnelByGroup(courseIds, memberIds));

        return new GroupCohortAnalyticsDto(
                group.getId(),
                group.getName(),
                memberIds.size(),
                courseIds.size(),
                funnel);
    }

    /** Global cohort analytics for platform admins. ADMIN role is enforced at the controller. */
    @Transactional(readOnly = true)
    public PlatformCohortAnalyticsDto getPlatformCohorts() {
        FunnelDto funnel = toFunnel(cohortRepository.funnelGlobal());
        List<MonthCountDto> cohorts = toMonthCounts(cohortRepository.enrollmentCohortsGlobal());
        List<MonthCountDto> certTrend = toMonthCounts(cohortRepository.certificateTrendGlobal());
        return new PlatformCohortAnalyticsDto(funnel, cohorts, certTrend);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private static FunnelDto toFunnel(FunnelProjection p) {
        // A scoped funnel query always returns one row; guard against null defensively.
        if (p == null) {
            return FunnelDto.empty();
        }
        return new FunnelDto(p.getEnrolled(), p.getStarted(), p.getCompleted(), p.getPassed(), p.getCertified());
    }

    private static List<MonthCountDto> toMonthCounts(List<MonthCountProjection> rows) {
        return rows.stream()
                .map(r -> new MonthCountDto(r.getMonth(), r.getTotal()))
                .toList();
    }

    private User resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof FirebaseAuthentication firebaseAuth)) {
            throw new IllegalStateException("Firebase authentication is required.");
        }
        return userRepository.findByFirebaseUid(firebaseAuth.getFirebaseUid())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found. Call /auth/sync first."));
    }
}

package com.edulife.analytics.controller;

import com.edulife.analytics.dto.GroupCohortAnalyticsDto;
import com.edulife.analytics.dto.PlatformCohortAnalyticsDto;
import com.edulife.analytics.dto.StudentProgressTrendDto;
import com.edulife.analytics.dto.TeacherCohortAnalyticsDto;
import com.edulife.analytics.service.CohortAnalyticsService;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only Phase C cohort/progress analytics endpoints. Firebase token + email_verified are
 * enforced globally by FirebaseTokenFilter; @PreAuthorize adds role gating and the service adds
 * ownership scoping. Each endpoint is covered by an RBAC controller test and a scoping service test.
 */
@RestController
@RequestMapping("/api/v1/analytics")
public class CohortAnalyticsController {

    private final CohortAnalyticsService cohortService;

    public CohortAnalyticsController(CohortAnalyticsService cohortService) {
        this.cohortService = cohortService;
    }

    /** Student's own progress trend. Any authenticated user; service scopes to the caller. */
    @GetMapping("/me/progress-trend")
    public StudentProgressTrendDto getMyProgressTrend() {
        return cohortService.getMyProgressTrend();
    }

    /** Teacher cohort analytics across owned courses. TEACHER/ADMIN; service scopes to owned courses. */
    @GetMapping("/teacher/cohorts")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public TeacherCohortAnalyticsDto getTeacherCohorts() {
        return cohortService.getMyTeacherCohorts();
    }

    /**
     * Group cohort analytics. GROUP_ADMIN/ADMIN at the role layer; the service additionally verifies
     * the caller owns the group (or is a platform admin), so a group admin cannot read another
     * group's cohort even with a valid groupId.
     */
    @GetMapping("/group/{groupId}/cohorts")
    @PreAuthorize("hasAnyRole('GROUP_ADMIN','ADMIN')")
    public GroupCohortAnalyticsDto getGroupCohorts(@PathVariable UUID groupId) {
        return cohortService.getGroupCohorts(groupId);
    }

    /** Global cohort analytics. ADMIN only. */
    @GetMapping("/platform/cohorts")
    @PreAuthorize("hasRole('ADMIN')")
    public PlatformCohortAnalyticsDto getPlatformCohorts() {
        return cohortService.getPlatformCohorts();
    }
}

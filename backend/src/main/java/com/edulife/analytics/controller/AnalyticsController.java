package com.edulife.analytics.controller;

import com.edulife.analytics.dto.PlatformAnalyticsDto;
import com.edulife.analytics.dto.StudentAnalyticsSummaryDto;
import com.edulife.analytics.dto.TeacherAnalyticsDto;
import com.edulife.analytics.service.AnalyticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only Phase A analytics endpoints. Firebase token validation and email_verified are
 * enforced globally by FirebaseTokenFilter, so every method here already has an authenticated,
 * verified caller. Role gating below is the second layer; the service adds ownership scoping.
 */
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Any authenticated learner can read their OWN summary. No role gate is needed because the
     * service scopes strictly to the resolved user id — there is no way to request another user.
     */
    @GetMapping("/me/summary")
    public StudentAnalyticsSummaryDto getMySummary() {
        return analyticsService.getMyStudentSummary();
    }

    /**
     * Teacher analytics — restricted to TEACHER/ADMIN. The service further scopes results to the
     * caller's own authored courses, so a teacher cannot read another teacher's metrics.
     */
    @GetMapping("/teacher/courses")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public TeacherAnalyticsDto getMyTeacherAnalytics() {
        return analyticsService.getMyTeacherAnalytics();
    }

    /** Global platform analytics — ADMIN only. */
    @GetMapping("/platform")
    @PreAuthorize("hasRole('ADMIN')")
    public PlatformAnalyticsDto getPlatformAnalytics() {
        return analyticsService.getPlatformAnalytics();
    }
}

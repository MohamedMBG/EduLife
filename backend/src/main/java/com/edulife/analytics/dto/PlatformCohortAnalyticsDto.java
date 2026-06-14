package com.edulife.analytics.dto;

import java.util.List;

/**
 * Global cohort analytics for platform admins: a platform-wide completion funnel, enrollment-month
 * cohorts, and a certificate issuance trend. ADMIN-only (enforced at the controller).
 */
public record PlatformCohortAnalyticsDto(
        FunnelDto funnel,
        List<MonthCountDto> enrollmentCohorts,
        List<MonthCountDto> certificateTrend
) {}

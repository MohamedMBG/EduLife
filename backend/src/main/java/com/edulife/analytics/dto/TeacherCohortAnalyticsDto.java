package com.edulife.analytics.dto;

import java.util.List;

/**
 * Cohort analytics aggregated across the requesting teacher's own courses: a completion funnel
 * plus enrollment-month cohorts. Scope (the set of owned course ids) is built server-side from
 * the resolved teacher; a teacher with no courses gets an empty funnel and empty cohort list.
 */
public record TeacherCohortAnalyticsDto(
        long courseCount,
        FunnelDto funnel,
        List<MonthCountDto> enrollmentCohorts
) {}

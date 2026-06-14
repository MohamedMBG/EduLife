package com.edulife.analytics.dto;

import java.util.UUID;

/**
 * Cohort analytics for one group: a completion funnel scoped to enrollments where BOTH the course
 * is attached to the group AND the learner is a group member. Ownership is enforced before this is
 * built (caller must be the group creator or a platform admin).
 */
public record GroupCohortAnalyticsDto(
        UUID groupId,
        String groupName,
        long memberCount,
        long courseCount,
        FunnelDto funnel
) {}

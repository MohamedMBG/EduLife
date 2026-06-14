package com.edulife.analytics.dto;

/**
 * Completion funnel for a scope. Counts are distinct (user, course) enrollment grains that
 * reached each stage; stages are non-increasing. See FunnelProjection for stage definitions.
 */
public record FunnelDto(
        long enrolled,
        long started,
        long completed,
        long passed,
        long certified
) {
    /** Zero funnel for empty scopes (teacher with no courses, group with no members/courses). */
    public static FunnelDto empty() {
        return new FunnelDto(0, 0, 0, 0, 0);
    }
}

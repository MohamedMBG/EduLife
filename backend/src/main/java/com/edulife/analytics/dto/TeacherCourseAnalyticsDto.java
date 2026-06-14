package com.edulife.analytics.dto;

import java.util.UUID;

/**
 * Aggregate performance for a single course owned by the requesting teacher.
 *
 * <p>Rates are percentages rounded to one decimal. {@code completionRatePercent} is the share
 * of learners-with-progress who finished every lesson. {@code passRatePercent} is attempt-based
 * (passed attempts / total attempts) and is documented as such because Phase A reads raw attempt
 * rows without per-learner deduplication — kept simple on purpose for a solo developer.</p>
 */
public record TeacherCourseAnalyticsDto(
        UUID courseId,
        String title,
        String status,
        long activeEnrollments,
        long learnersWithProgress,
        long learnersCompleted,
        double completionRatePercent,
        long examAttempts,
        long examsPassed,
        double passRatePercent,
        long certificatesIssued
) {}

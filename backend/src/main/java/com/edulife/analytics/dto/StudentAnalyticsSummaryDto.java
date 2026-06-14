package com.edulife.analytics.dto;

/**
 * A student's own learning summary. Every field is scoped to the authenticated user only —
 * the service resolves the internal user id server-side, so one student can never read
 * another student's counts. No PII, no firebase_uid, no exam answers are included.
 */
public record StudentAnalyticsSummaryDto(
        long activeEnrollments,
        long lessonsCompleted,
        long examAttempts,
        long examsPassed,
        long certificatesEarned
) {}

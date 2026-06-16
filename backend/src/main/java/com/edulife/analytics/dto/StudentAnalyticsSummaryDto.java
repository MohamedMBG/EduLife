package com.edulife.analytics.dto;

/**
 * A student's own learning summary. Every field is scoped to the authenticated user only —
 * the service resolves the internal user id server-side, so one student can never read
 * another student's counts. No PII, no firebase_uid, no exam answers are included.
 *
 * Scores are 0-100 integer percentages aggregated over the learner's own exam attempts;
 * a learner with no attempts gets 0 for both, which avoids divide-by-zero on the client.
 */
public record StudentAnalyticsSummaryDto(
        long activeEnrollments,
        long lessonsCompleted,
        long examAttempts,
        long examsPassed,
        long certificatesEarned,
        int averageExamScore,
        int bestExamScore
) {}

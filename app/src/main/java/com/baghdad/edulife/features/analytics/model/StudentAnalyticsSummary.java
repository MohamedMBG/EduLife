package com.baghdad.edulife.features.analytics.model;

/**
 * Student's own learning summary. Mirrors the backend StudentAnalyticsSummaryDto returned by
 * GET /api/v1/analytics/me/summary. The backend scopes this strictly to the authenticated user,
 * so the client never sends or trusts any user id — it just renders what the server returns.
 */
public class StudentAnalyticsSummary {
    public long activeEnrollments;
    public long lessonsCompleted;
    public long examAttempts;
    public long examsPassed;
    public long certificatesEarned;
    public int averageExamScore;
    public int bestExamScore;
}

package com.baghdad.edulife.features.analytics.model;

/**
 * Global platform counts. Mirrors the backend PlatformAnalyticsDto from
 * GET /api/v1/analytics/platform. The endpoint is ADMIN-only server-side; the client renders
 * whatever it receives and never gates access itself.
 */
public class PlatformAnalytics {
    public long learners;
    public long teachers;
    public long groupAdmins;
    public long admins;
    public long coursesDraft;
    public long coursesPublished;
    public long coursesArchived;
    public long activeEnrollments;
    public long totalExamAttempts;
    public long totalExamsPassed;
    public long totalCertificates;
}

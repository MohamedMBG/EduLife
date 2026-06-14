package com.baghdad.edulife.features.analytics.model;

/**
 * Per-course performance for a course owned by the requesting teacher. Mirrors the backend
 * TeacherCourseAnalyticsDto. passRatePercent is attempt-based (documented server-side); the
 * client only displays the numbers, it does not recompute scope or ownership.
 */
public class TeacherCourseAnalytics {
    public String courseId;
    public String title;
    public String status;
    public long activeEnrollments;
    public long learnersWithProgress;
    public long learnersCompleted;
    public double completionRatePercent;
    public long examAttempts;
    public long examsPassed;
    public double passRatePercent;
    public long certificatesIssued;
}

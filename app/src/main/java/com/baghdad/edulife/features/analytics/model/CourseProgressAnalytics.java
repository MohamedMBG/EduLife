package com.baghdad.edulife.features.analytics.model;

/**
 * Per-course progress card on the Study Analytics screen. Built from the caller's real
 * enrollments (/enrollments/me) and per-course progress (/progress/courses/{id}).
 */
public class CourseProgressAnalytics {
    public final String courseTitle;
    public final int lessonsCompleted;
    public final int totalLessons;
    public final String lastActivity;

    public CourseProgressAnalytics(String courseTitle, int lessonsCompleted, int totalLessons,
                                   String lastActivity) {
        this.courseTitle = courseTitle;
        this.lessonsCompleted = lessonsCompleted;
        this.totalLessons = totalLessons;
        this.lastActivity = lastActivity;
    }

    /** Completion percentage clamped to 0–100; guards against a zero total. */
    public int progressPercent() {
        if (totalLessons <= 0) return 0;
        int pct = Math.round((lessonsCompleted * 100f) / totalLessons);
        return Math.max(0, Math.min(100, pct));
    }
}

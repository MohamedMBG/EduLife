package com.baghdad.edulife.features.analytics.model;

import java.util.List;

/**
 * Aggregate model backing the redesigned Study Analytics screen. It bundles the overall summary,
 * weekly activity, per-course progress, exam performance, streak, and a few insight lines so the
 * fragment binds a single object.
 *
 * All values are sourced from the backend: totals + exam avg/best from /analytics/me/summary,
 * per-course progress and weekly activity from /enrollments/me + /progress/courses/{id}, streak
 * from /gamification/me. Insights are derived locally from those values.
 */
public class StudyAnalytics {

    public final int overallProgressPercent;
    public final String currentPathTitle;
    public final long completedLessons;
    public final long activeCourses;
    public final long certificatesEarned;

    public final WeeklyStudyActivity weekly;
    public final List<CourseProgressAnalytics> courses;
    public final ExamPerformanceSummary exam;
    public final LearningStreakSummary streak;
    public final List<AnalyticsInsight> insights;

    public StudyAnalytics(int overallProgressPercent, String currentPathTitle, long completedLessons,
                          long activeCourses, long certificatesEarned, WeeklyStudyActivity weekly,
                          List<CourseProgressAnalytics> courses, ExamPerformanceSummary exam,
                          LearningStreakSummary streak, List<AnalyticsInsight> insights) {
        this.overallProgressPercent = overallProgressPercent;
        this.currentPathTitle = currentPathTitle;
        this.completedLessons = completedLessons;
        this.activeCourses = activeCourses;
        this.certificatesEarned = certificatesEarned;
        this.weekly = weekly;
        this.courses = courses;
        this.exam = exam;
        this.streak = streak;
        this.insights = insights;
    }
}

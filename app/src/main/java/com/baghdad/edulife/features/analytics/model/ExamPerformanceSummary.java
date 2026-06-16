package com.baghdad.edulife.features.analytics.model;

/**
 * Exam performance block: average score, passes and best score. All three fields come straight
 * from /analytics/me/summary — average and best are 0-100 integer percentages aggregated server-side
 * over the caller's own exam attempts.
 */
public class ExamPerformanceSummary {
    public final int averageScore;
    public final int passedExams;
    public final int bestScore;

    public ExamPerformanceSummary(int averageScore, int passedExams, int bestScore) {
        this.averageScore = averageScore;
        this.passedExams = passedExams;
        this.bestScore = bestScore;
    }
}

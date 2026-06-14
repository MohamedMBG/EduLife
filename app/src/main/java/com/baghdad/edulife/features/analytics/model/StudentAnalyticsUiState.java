package com.baghdad.edulife.features.analytics.model;

/**
 * Immutable UI state for the student analytics screen. The fragment renders exactly one of
 * loading / error / success, matching the four required states (empty is folded into success
 * since a synced learner always gets a summary object, possibly all-zero).
 */
public class StudentAnalyticsUiState {
    public final boolean loading;
    public final StudentAnalyticsSummary summary;
    public final String errorMessage;

    private StudentAnalyticsUiState(boolean loading, StudentAnalyticsSummary summary, String errorMessage) {
        this.loading = loading;
        this.summary = summary;
        this.errorMessage = errorMessage;
    }

    public static StudentAnalyticsUiState loading() {
        return new StudentAnalyticsUiState(true, null, null);
    }

    public static StudentAnalyticsUiState success(StudentAnalyticsSummary summary) {
        return new StudentAnalyticsUiState(false, summary, null);
    }

    public static StudentAnalyticsUiState error(String message) {
        return new StudentAnalyticsUiState(false, null, message);
    }
}

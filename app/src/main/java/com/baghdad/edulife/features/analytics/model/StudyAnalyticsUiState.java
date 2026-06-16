package com.baghdad.edulife.features.analytics.model;

/**
 * Immutable UI state for the redesigned Study Analytics screen. Exactly one of
 * loading / error / success is represented. Empty is folded into success: a learner with no
 * activity still receives a {@link StudyAnalytics} object with zeroed values, rendered as a gentle
 * "start learning" state rather than a separate empty screen.
 */
public class StudyAnalyticsUiState {
    public final boolean loading;
    public final StudyAnalytics analytics;
    public final String errorMessage;

    private StudyAnalyticsUiState(boolean loading, StudyAnalytics analytics, String errorMessage) {
        this.loading = loading;
        this.analytics = analytics;
        this.errorMessage = errorMessage;
    }

    public static StudyAnalyticsUiState loading() {
        return new StudyAnalyticsUiState(true, null, null);
    }

    public static StudyAnalyticsUiState success(StudyAnalytics analytics) {
        return new StudyAnalyticsUiState(false, analytics, null);
    }

    public static StudyAnalyticsUiState error(String message) {
        return new StudyAnalyticsUiState(false, null, message);
    }
}

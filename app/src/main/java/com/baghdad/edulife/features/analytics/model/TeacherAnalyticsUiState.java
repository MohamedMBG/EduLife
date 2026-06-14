package com.baghdad.edulife.features.analytics.model;

/**
 * Immutable UI state for the teacher analytics screen. Empty is distinct here: a teacher with no
 * owned courses gets a success state whose course list is empty, which the fragment renders as
 * the empty view rather than the list.
 */
public class TeacherAnalyticsUiState {
    public final boolean loading;
    public final TeacherAnalytics data;
    public final String errorMessage;

    private TeacherAnalyticsUiState(boolean loading, TeacherAnalytics data, String errorMessage) {
        this.loading = loading;
        this.data = data;
        this.errorMessage = errorMessage;
    }

    public static TeacherAnalyticsUiState loading() {
        return new TeacherAnalyticsUiState(true, null, null);
    }

    public static TeacherAnalyticsUiState success(TeacherAnalytics data) {
        return new TeacherAnalyticsUiState(false, data, null);
    }

    public static TeacherAnalyticsUiState error(String message) {
        return new TeacherAnalyticsUiState(false, null, message);
    }
}

package com.baghdad.edulife.features.analytics.model;

/** Immutable UI state for the platform analytics screen. */
public class PlatformAnalyticsUiState {
    public final boolean loading;
    public final PlatformAnalytics data;
    public final String errorMessage;

    private PlatformAnalyticsUiState(boolean loading, PlatformAnalytics data, String errorMessage) {
        this.loading = loading;
        this.data = data;
        this.errorMessage = errorMessage;
    }

    public static PlatformAnalyticsUiState loading() {
        return new PlatformAnalyticsUiState(true, null, null);
    }

    public static PlatformAnalyticsUiState success(PlatformAnalytics data) {
        return new PlatformAnalyticsUiState(false, data, null);
    }

    public static PlatformAnalyticsUiState error(String message) {
        return new PlatformAnalyticsUiState(false, null, message);
    }
}

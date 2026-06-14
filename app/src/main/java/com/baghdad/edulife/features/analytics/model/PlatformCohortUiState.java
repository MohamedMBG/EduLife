package com.baghdad.edulife.features.analytics.model;

/** Loading/error/success state for the platform cohort analytics section. */
public class PlatformCohortUiState {
    public final boolean loading;
    public final PlatformCohortAnalytics data;
    public final String errorMessage;

    private PlatformCohortUiState(boolean loading, PlatformCohortAnalytics data, String errorMessage) {
        this.loading = loading;
        this.data = data;
        this.errorMessage = errorMessage;
    }

    public static PlatformCohortUiState loading() {
        return new PlatformCohortUiState(true, null, null);
    }

    public static PlatformCohortUiState success(PlatformCohortAnalytics data) {
        return new PlatformCohortUiState(false, data, null);
    }

    public static PlatformCohortUiState error(String message) {
        return new PlatformCohortUiState(false, null, message);
    }
}

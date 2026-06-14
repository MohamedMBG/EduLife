package com.baghdad.edulife.features.analytics.model;

/** Loading/error/success state for the student progress-trend section. */
public class StudentTrendUiState {
    public final boolean loading;
    public final StudentProgressTrend trend;
    public final String errorMessage;

    private StudentTrendUiState(boolean loading, StudentProgressTrend trend, String errorMessage) {
        this.loading = loading;
        this.trend = trend;
        this.errorMessage = errorMessage;
    }

    public static StudentTrendUiState loading() {
        return new StudentTrendUiState(true, null, null);
    }

    public static StudentTrendUiState success(StudentProgressTrend trend) {
        return new StudentTrendUiState(false, trend, null);
    }

    public static StudentTrendUiState error(String message) {
        return new StudentTrendUiState(false, null, message);
    }
}

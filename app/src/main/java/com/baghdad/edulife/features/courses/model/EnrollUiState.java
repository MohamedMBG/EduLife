package com.baghdad.edulife.features.courses.model;

public class EnrollUiState {
    public final boolean loading;
    public final boolean enrolled;
    public final String errorMessage;

    private EnrollUiState(boolean loading, boolean enrolled, String errorMessage) {
        this.loading = loading;
        this.enrolled = enrolled;
        this.errorMessage = errorMessage;
    }

    public static EnrollUiState idle() {
        return new EnrollUiState(false, false, null);
    }

    public static EnrollUiState loading() {
        return new EnrollUiState(true, false, null);
    }

    public static EnrollUiState success() {
        return new EnrollUiState(false, true, null);
    }

    public static EnrollUiState error(String message) {
        return new EnrollUiState(false, false, message);
    }
}

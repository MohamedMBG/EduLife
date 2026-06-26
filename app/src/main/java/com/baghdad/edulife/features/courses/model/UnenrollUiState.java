package com.baghdad.edulife.features.courses.model;

/** Immutable UI state for the course unenrollment action, representing idle, loading, success, or error states. */
public class UnenrollUiState {
    public final boolean loading;
    public final boolean unenrolled;
    public final String errorMessage;

    private UnenrollUiState(boolean loading, boolean unenrolled, String errorMessage) {
        this.loading = loading;
        this.unenrolled = unenrolled;
        this.errorMessage = errorMessage;
    }

    public static UnenrollUiState idle() {
        return new UnenrollUiState(false, false, null);
    }

    public static UnenrollUiState loading() {
        return new UnenrollUiState(true, false, null);
    }

    public static UnenrollUiState success() {
        return new UnenrollUiState(false, true, null);
    }

    public static UnenrollUiState error(String message) {
        return new UnenrollUiState(false, false, message);
    }
}

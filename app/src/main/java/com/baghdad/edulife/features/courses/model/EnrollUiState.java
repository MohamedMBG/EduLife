package com.baghdad.edulife.features.courses.model;

public class EnrollUiState {
    public final boolean loading;
    public final boolean enrolled;
    /**
     * True when the backend reported the learner was already enrolled (HTTP 409). The UI should
     * still navigate to My Courses, but show "already enrolled" instead of "enrolled" so a
     * silent re-tap does not produce a misleading success toast.
     */
    public final boolean alreadyEnrolled;
    public final String errorMessage;

    private EnrollUiState(boolean loading, boolean enrolled, boolean alreadyEnrolled, String errorMessage) {
        this.loading = loading;
        this.enrolled = enrolled;
        this.alreadyEnrolled = alreadyEnrolled;
        this.errorMessage = errorMessage;
    }

    public static EnrollUiState idle() {
        return new EnrollUiState(false, false, false, null);
    }

    public static EnrollUiState loading() {
        return new EnrollUiState(true, false, false, null);
    }

    public static EnrollUiState success() {
        return new EnrollUiState(false, true, false, null);
    }

    public static EnrollUiState alreadyEnrolled() {
        return new EnrollUiState(false, true, true, null);
    }

    public static EnrollUiState error(String message) {
        return new EnrollUiState(false, false, false, message);
    }
}

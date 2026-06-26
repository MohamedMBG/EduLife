package com.baghdad.edulife.features.courses.model;

/** Immutable UI state for exam submission, covering loading, success, already-passed, cooldown, and error outcomes. */
public class ExamSubmitUiState {
    public final boolean loading;
    public final ExamResultResponse result;
    public final String errorMessage;
    public final boolean alreadyPassed;
    public final String cooldownEndsAt;

    private ExamSubmitUiState(boolean loading, ExamResultResponse result, String errorMessage,
                               boolean alreadyPassed, String cooldownEndsAt) {
        this.loading = loading;
        this.result = result;
        this.errorMessage = errorMessage;
        this.alreadyPassed = alreadyPassed;
        this.cooldownEndsAt = cooldownEndsAt;
    }

    public static ExamSubmitUiState idle() {
        return new ExamSubmitUiState(false, null, null, false, null);
    }

    public static ExamSubmitUiState loading() {
        return new ExamSubmitUiState(true, null, null, false, null);
    }

    public static ExamSubmitUiState success(ExamResultResponse result) {
        return new ExamSubmitUiState(false, result, null, false, null);
    }

    public static ExamSubmitUiState alreadyPassed() {
        return new ExamSubmitUiState(false, null, null, true, null);
    }

    public static ExamSubmitUiState cooldown(String cooldownEndsAt) {
        return new ExamSubmitUiState(false, null, null, false, cooldownEndsAt);
    }

    public static ExamSubmitUiState error(String msg) {
        return new ExamSubmitUiState(false, null, msg, false, null);
    }
}

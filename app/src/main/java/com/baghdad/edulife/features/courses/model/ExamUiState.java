package com.baghdad.edulife.features.courses.model;

/** Immutable UI state for loading an exam, covering loading, success, already-passed, cooldown, and error outcomes. */
public class ExamUiState {
    public final boolean loading;
    public final ExamResponse exam;
    public final String errorMessage;
    public final boolean alreadyPassed;
    public final String cooldownEndsAt;

    private ExamUiState(boolean loading, ExamResponse exam, String errorMessage,
                        boolean alreadyPassed, String cooldownEndsAt) {
        this.loading = loading;
        this.exam = exam;
        this.errorMessage = errorMessage;
        this.alreadyPassed = alreadyPassed;
        this.cooldownEndsAt = cooldownEndsAt;
    }

    public static ExamUiState idle() {
        return new ExamUiState(false, null, null, false, null);
    }

    public static ExamUiState loading() {
        return new ExamUiState(true, null, null, false, null);
    }

    public static ExamUiState success(ExamResponse exam) {
        return new ExamUiState(false, exam, null, false, null);
    }

    public static ExamUiState alreadyPassed() {
        return new ExamUiState(false, null, null, true, null);
    }

    public static ExamUiState cooldown(String cooldownEndsAt) {
        return new ExamUiState(false, null, null, false, cooldownEndsAt);
    }

    public static ExamUiState error(String msg) {
        return new ExamUiState(false, null, msg, false, null);
    }
}

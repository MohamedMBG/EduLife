package com.baghdad.edulife.features.courses.model;

public class ExamStatusUiState {
    public final boolean loading;
    public final ExamStatusResponse status;
    public final String errorMessage;

    private ExamStatusUiState(boolean loading, ExamStatusResponse status, String errorMessage) {
        this.loading = loading;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public static ExamStatusUiState idle() {
        return new ExamStatusUiState(false, null, null);
    }

    public static ExamStatusUiState loading() {
        return new ExamStatusUiState(true, null, null);
    }

    public static ExamStatusUiState success(ExamStatusResponse status) {
        return new ExamStatusUiState(false, status, null);
    }

    public static ExamStatusUiState error(String message) {
        return new ExamStatusUiState(false, null, message);
    }
}

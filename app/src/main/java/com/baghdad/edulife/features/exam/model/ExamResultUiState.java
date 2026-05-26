package com.baghdad.edulife.features.exam.model;

public class ExamResultUiState {
    public final boolean loading;
    public final ExamResultDto result;
    public final String errorMessage;

    private ExamResultUiState(boolean loading, ExamResultDto result, String errorMessage) {
        this.loading = loading;
        this.result = result;
        this.errorMessage = errorMessage;
    }

    public static ExamResultUiState idle() {
        return new ExamResultUiState(false, null, null);
    }

    public static ExamResultUiState loading() {
        return new ExamResultUiState(true, null, null);
    }

    public static ExamResultUiState success(ExamResultDto result) {
        return new ExamResultUiState(false, result, null);
    }

    public static ExamResultUiState error(String message) {
        return new ExamResultUiState(false, null, message);
    }
}

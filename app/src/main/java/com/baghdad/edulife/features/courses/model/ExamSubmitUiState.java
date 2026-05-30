package com.baghdad.edulife.features.courses.model;

public class ExamSubmitUiState {
    public final boolean loading;
    public final ExamResultResponse result;
    public final String errorMessage;

    private ExamSubmitUiState(boolean loading, ExamResultResponse result, String errorMessage) {
        this.loading = loading;
        this.result = result;
        this.errorMessage = errorMessage;
    }

    public static ExamSubmitUiState idle() {
        return new ExamSubmitUiState(false, null, null);
    }

    public static ExamSubmitUiState loading() {
        return new ExamSubmitUiState(true, null, null);
    }

    public static ExamSubmitUiState success(ExamResultResponse result) {
        return new ExamSubmitUiState(false, result, null);
    }

    public static ExamSubmitUiState error(String msg) {
        return new ExamSubmitUiState(false, null, msg);
    }
}

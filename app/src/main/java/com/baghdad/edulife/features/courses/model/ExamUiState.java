package com.baghdad.edulife.features.courses.model;

public class ExamUiState {
    public final boolean loading;
    public final ExamResponse exam;
    public final String errorMessage;

    private ExamUiState(boolean loading, ExamResponse exam, String errorMessage) {
        this.loading = loading;
        this.exam = exam;
        this.errorMessage = errorMessage;
    }

    public static ExamUiState idle() {
        return new ExamUiState(false, null, null);
    }

    public static ExamUiState loading() {
        return new ExamUiState(true, null, null);
    }

    public static ExamUiState success(ExamResponse exam) {
        return new ExamUiState(false, exam, null);
    }

    public static ExamUiState error(String msg) {
        return new ExamUiState(false, null, msg);
    }
}

package com.baghdad.edulife.features.exam.model;

public class ExamUiState {
    public final boolean loading;
    public final ExamDto exam;
    public final boolean submitting;
    public final String errorMessage;

    private ExamUiState(boolean loading, ExamDto exam, boolean submitting, String errorMessage) {
        this.loading = loading;
        this.exam = exam;
        this.submitting = submitting;
        this.errorMessage = errorMessage;
    }

    public static ExamUiState idle() {
        return new ExamUiState(false, null, false, null);
    }

    public static ExamUiState loading() {
        return new ExamUiState(true, null, false, null);
    }

    public static ExamUiState success(ExamDto exam) {
        return new ExamUiState(false, exam, false, null);
    }

    public static ExamUiState submitting(ExamDto exam) {
        return new ExamUiState(false, exam, true, null);
    }

    public static ExamUiState error(String message) {
        return new ExamUiState(false, null, false, message);
    }
}

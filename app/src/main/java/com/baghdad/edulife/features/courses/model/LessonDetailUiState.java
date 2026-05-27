package com.baghdad.edulife.features.courses.model;

public class LessonDetailUiState {
    public final boolean loading;
    public final LessonDetail lessonDetail;
    public final String errorMessage;

    private LessonDetailUiState(boolean loading, LessonDetail lessonDetail, String errorMessage) {
        this.loading = loading;
        this.lessonDetail = lessonDetail;
        this.errorMessage = errorMessage;
    }

    public static LessonDetailUiState idle() {
        return new LessonDetailUiState(false, null, null);
    }

    public static LessonDetailUiState loading() {
        return new LessonDetailUiState(true, null, null);
    }

    public static LessonDetailUiState success(LessonDetail lessonDetail) {
        return new LessonDetailUiState(false, lessonDetail, null);
    }

    public static LessonDetailUiState error(String message) {
        return new LessonDetailUiState(false, null, message);
    }
}

package com.baghdad.edulife.features.courses.model;

public class CourseProgressUiState {
    public final boolean loading;
    public final CourseProgress progress;
    public final String errorMessage;

    private CourseProgressUiState(boolean loading, CourseProgress progress, String errorMessage) {
        this.loading = loading;
        this.progress = progress;
        this.errorMessage = errorMessage;
    }

    public static CourseProgressUiState idle() {
        return new CourseProgressUiState(false, null, null);
    }

    public static CourseProgressUiState loading() {
        return new CourseProgressUiState(true, null, null);
    }

    public static CourseProgressUiState success(CourseProgress progress) {
        return new CourseProgressUiState(false, progress, null);
    }

    public static CourseProgressUiState error(String message) {
        return new CourseProgressUiState(false, null, message);
    }
}

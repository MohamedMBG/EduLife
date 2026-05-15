package com.baghdad.edulife.features.courses.model;

public class CourseDetailUiState {
    public final boolean loading;
    public final CourseDetail courseDetail;
    public final String errorMessage;

    private CourseDetailUiState(boolean loading, CourseDetail courseDetail, String errorMessage) {
        this.loading = loading;
        this.courseDetail = courseDetail;
        this.errorMessage = errorMessage;
    }

    public static CourseDetailUiState idle() {
        return new CourseDetailUiState(false, null, null);
    }

    public static CourseDetailUiState loading() {
        return new CourseDetailUiState(true, null, null);
    }

    public static CourseDetailUiState success(CourseDetail courseDetail) {
        return new CourseDetailUiState(false, courseDetail, null);
    }

    public static CourseDetailUiState error(String message) {
        return new CourseDetailUiState(false, null, message);
    }
}

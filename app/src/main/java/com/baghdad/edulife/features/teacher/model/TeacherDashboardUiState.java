package com.baghdad.edulife.features.teacher.model;

import java.util.List;

public class TeacherDashboardUiState {
    public final boolean loading;
    public final List<CmsCourse> courses;
    public final String error;

    private TeacherDashboardUiState(boolean loading, List<CmsCourse> courses, String error) {
        this.loading = loading;
        this.courses = courses;
        this.error = error;
    }

    public static TeacherDashboardUiState loading() {
        return new TeacherDashboardUiState(true, null, null);
    }

    public static TeacherDashboardUiState success(List<CmsCourse> courses) {
        return new TeacherDashboardUiState(false, courses, null);
    }

    public static TeacherDashboardUiState error(String error) {
        return new TeacherDashboardUiState(false, null, error);
    }
}

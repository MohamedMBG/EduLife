package com.baghdad.edulife.features.admin.model;

import java.util.List;

public class TeacherRequestsUiState {
    public final boolean loading;
    public final List<AdminTeacherRequest> requests;
    public final String errorMessage;
    public final String actionMessage;

    private TeacherRequestsUiState(boolean loading, List<AdminTeacherRequest> requests,
                                    String errorMessage, String actionMessage) {
        this.loading = loading;
        this.requests = requests;
        this.errorMessage = errorMessage;
        this.actionMessage = actionMessage;
    }

    public static TeacherRequestsUiState loading() {
        return new TeacherRequestsUiState(true, null, null, null);
    }

    public static TeacherRequestsUiState success(List<AdminTeacherRequest> requests) {
        return new TeacherRequestsUiState(false, requests, null, null);
    }

    public static TeacherRequestsUiState error(String message) {
        return new TeacherRequestsUiState(false, null, message, null);
    }

    public static TeacherRequestsUiState withAction(List<AdminTeacherRequest> requests, String message) {
        return new TeacherRequestsUiState(false, requests, null, message);
    }
}

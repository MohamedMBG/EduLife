package com.baghdad.edulife.features.admin.model;

public class AdminUiState {
    public final boolean loading;
    public final AdminStats stats;
    public final String errorMessage;

    private AdminUiState(boolean loading, AdminStats stats, String errorMessage) {
        this.loading = loading;
        this.stats = stats;
        this.errorMessage = errorMessage;
    }

    public static AdminUiState loading() {
        return new AdminUiState(true, null, null);
    }

    public static AdminUiState success(AdminStats stats) {
        return new AdminUiState(false, stats, null);
    }

    public static AdminUiState error(String message) {
        return new AdminUiState(false, null, message);
    }
}

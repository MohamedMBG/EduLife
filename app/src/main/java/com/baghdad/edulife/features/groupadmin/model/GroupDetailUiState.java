package com.baghdad.edulife.features.groupadmin.model;

/** UI state for a single group's detail screen (members + attached courses). */
public class GroupDetailUiState {
    public final boolean loading;
    public final GroupDetail detail;
    public final String error;

    private GroupDetailUiState(boolean loading, GroupDetail detail, String error) {
        this.loading = loading;
        this.detail = detail;
        this.error = error;
    }

    public static GroupDetailUiState loading() {
        return new GroupDetailUiState(true, null, null);
    }

    public static GroupDetailUiState success(GroupDetail detail) {
        return new GroupDetailUiState(false, detail, null);
    }

    public static GroupDetailUiState error(String error) {
        return new GroupDetailUiState(false, null, error);
    }
}

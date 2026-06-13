package com.baghdad.edulife.features.groupadmin.model;

import java.util.List;

/** UI state for the group admin dashboard (the list of owned groups). */
public class GroupAdminUiState {
    public final boolean loading;
    public final List<GroupSummary> groups;
    public final String error;

    private GroupAdminUiState(boolean loading, List<GroupSummary> groups, String error) {
        this.loading = loading;
        this.groups = groups;
        this.error = error;
    }

    public static GroupAdminUiState loading() {
        return new GroupAdminUiState(true, null, null);
    }

    public static GroupAdminUiState success(List<GroupSummary> groups) {
        return new GroupAdminUiState(false, groups, null);
    }

    public static GroupAdminUiState error(String error) {
        return new GroupAdminUiState(false, null, error);
    }
}

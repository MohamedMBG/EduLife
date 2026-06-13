package com.baghdad.edulife.features.groupadmin.model;

import com.baghdad.edulife.features.teacher.model.CmsCourse;

import java.util.List;

/**
 * UI state for the course approvals queue. The same CMS course list is split into the
 * pending (DRAFT) review queue and the already published courses for display.
 */
public class ApprovalsUiState {
    public final boolean loading;
    public final List<CmsCourse> pending;
    public final List<CmsCourse> published;
    public final String error;

    private ApprovalsUiState(boolean loading, List<CmsCourse> pending,
                             List<CmsCourse> published, String error) {
        this.loading = loading;
        this.pending = pending;
        this.published = published;
        this.error = error;
    }

    public static ApprovalsUiState loading() {
        return new ApprovalsUiState(true, null, null, null);
    }

    public static ApprovalsUiState success(List<CmsCourse> pending, List<CmsCourse> published) {
        return new ApprovalsUiState(false, pending, published, null);
    }

    public static ApprovalsUiState error(String error) {
        return new ApprovalsUiState(false, null, null, error);
    }
}

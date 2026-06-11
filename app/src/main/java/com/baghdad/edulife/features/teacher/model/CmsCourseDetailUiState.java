package com.baghdad.edulife.features.teacher.model;

import java.util.List;

public class CmsCourseDetailUiState {
    public final boolean loading;
    public final CmsCourse course;
    public final List<CmsSection> sections;
    public final String error;
    public final String actionMessage;

    private CmsCourseDetailUiState(boolean loading, CmsCourse course,
                                    List<CmsSection> sections, String error,
                                    String actionMessage) {
        this.loading = loading;
        this.course = course;
        this.sections = sections;
        this.error = error;
        this.actionMessage = actionMessage;
    }

    public static CmsCourseDetailUiState loading() {
        return new CmsCourseDetailUiState(true, null, null, null, null);
    }

    public static CmsCourseDetailUiState success(CmsCourse course, List<CmsSection> sections) {
        return new CmsCourseDetailUiState(false, course, sections, null, null);
    }

    public static CmsCourseDetailUiState error(String error) {
        return new CmsCourseDetailUiState(false, null, null, error, null);
    }

    public CmsCourseDetailUiState withActionMessage(String message) {
        return new CmsCourseDetailUiState(false, this.course, this.sections, this.error, message);
    }
}

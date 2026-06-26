package com.baghdad.edulife.features.courses.model;

import java.util.Collections;
import java.util.List;

/**
 * Immutable UI state for the course catalog screen, tracking the loaded course list, active category filter, and loading/error status.
 */
public class CourseCatalogUiState {
    public final boolean loading;
    public final List<CourseSummary> courses;
    public final String errorMessage;
    public final String selectedCategory;

    private CourseCatalogUiState(
            boolean loading,
            List<CourseSummary> courses,
            String errorMessage,
            String selectedCategory
    ) {
        this.loading = loading;
        this.courses = courses;
        this.errorMessage = errorMessage;
        this.selectedCategory = selectedCategory;
    }

    public static CourseCatalogUiState idle() {
        return new CourseCatalogUiState(false, Collections.emptyList(), null, null);
    }

    public static CourseCatalogUiState loading(String selectedCategory) {
        return new CourseCatalogUiState(true, Collections.emptyList(), null, selectedCategory);
    }

    public static CourseCatalogUiState success(List<CourseSummary> courses, String selectedCategory) {
        return new CourseCatalogUiState(false, courses, null, selectedCategory);
    }

    public static CourseCatalogUiState error(String message, String selectedCategory) {
        return new CourseCatalogUiState(false, Collections.emptyList(), message, selectedCategory);
    }
}

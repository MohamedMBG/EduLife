package com.edulife.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/v1/cms/courses.
 * Slug is derived server-side from title to prevent callers from injecting arbitrary slugs.
 */
public record CreateCourseRequest(
        @NotBlank(message = "title is required")
        @Size(max = 255, message = "title must be 255 characters or fewer")
        String title,

        @Size(max = 500, message = "shortDescription must be 500 characters or fewer")
        String shortDescription,

        @NotBlank(message = "description is required")
        String description,

        @NotBlank(message = "languageCode is required")
        @Size(max = 10, message = "languageCode must be 10 characters or fewer")
        String languageCode,

        // level maps to the `level` column, which also serves as the catalog category filter.
        String level,

        String imageUrl
) {}

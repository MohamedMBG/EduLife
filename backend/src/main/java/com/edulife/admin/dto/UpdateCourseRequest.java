package com.edulife.admin.dto;

import jakarta.validation.constraints.Size;

/**
 * Request body for PUT /api/v1/cms/courses/{id}.
 * All fields are optional so callers can update only what changed.
 * Slug and status are immutable through this endpoint.
 */
public record UpdateCourseRequest(
        @Size(max = 255, message = "title must be 255 characters or fewer")
        String title,

        @Size(max = 500, message = "shortDescription must be 500 characters or fewer")
        String shortDescription,

        String description,

        @Size(max = 10, message = "languageCode must be 10 characters or fewer")
        String languageCode,

        String level,

        String imageUrl
) {}

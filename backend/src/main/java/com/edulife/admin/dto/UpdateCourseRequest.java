package com.edulife.admin.dto;

import jakarta.validation.constraints.Pattern;
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

        @Size(max = 20000, message = "description must be 20000 characters or fewer")
        String description,

        @Size(max = 10, message = "languageCode must be 10 characters or fewer")
        String languageCode,

        String level,

        // Only http(s) URLs accepted; other schemes (javascript:, data:) block stored-XSS via the
        // cover image rendered as <img src> by both clients.
        @Size(max = 2048, message = "imageUrl must be 2048 characters or fewer")
        @Pattern(regexp = "^(https?://.+)?$", message = "imageUrl must be an http(s) URL")
        String imageUrl
) {}

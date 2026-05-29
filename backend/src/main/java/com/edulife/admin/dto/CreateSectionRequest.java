package com.edulife.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request body for POST /api/v1/cms/courses/{courseId}/sections. */
public record CreateSectionRequest(
        @NotBlank(message = "title is required")
        @Size(max = 255, message = "title must be 255 characters or fewer")
        String title,

        String description,

        // displayOrder must be > 0 per the DB constraint on course_sections.
        @NotNull(message = "displayOrder is required")
        @Min(value = 1, message = "displayOrder must be at least 1")
        Integer displayOrder
) {}

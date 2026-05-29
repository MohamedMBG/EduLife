package com.edulife.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Request body for PUT /api/v1/cms/courses/{courseId}/sections/{sectionId}. All fields optional. */
public record UpdateSectionRequest(
        @Size(max = 255, message = "title must be 255 characters or fewer")
        String title,

        String description,

        @Min(value = 1, message = "displayOrder must be at least 1")
        Integer displayOrder
) {}

package com.edulife.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request body for POST /api/v1/cms/sections/{sectionId}/lessons. */
public record CreateLessonRequest(
        @NotBlank(message = "title is required")
        @Size(max = 255, message = "title must be 255 characters or fewer")
        String title,

        String summary,

        // Matches the DB constraint: VIDEO, ARTICLE, or RESOURCE.
        @NotBlank(message = "lessonType is required")
        @Pattern(regexp = "VIDEO|ARTICLE|RESOURCE", message = "lessonType must be VIDEO, ARTICLE, or RESOURCE")
        String lessonType,

        @Min(value = 1, message = "estimatedDurationMinutes must be at least 1")
        Integer estimatedDurationMinutes,

        @NotNull(message = "displayOrder is required")
        @Min(value = 1, message = "displayOrder must be at least 1")
        Integer displayOrder,

        boolean preview,

        // contentUrl and contentBody are mutually exclusive by convention — VIDEO uses URL, ARTICLE uses body.
        String contentUrl,
        String contentBody
) {}

package com.edulife.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request body for PUT /api/v1/cms/sections/{sectionId}/lessons/{lessonId}. All fields optional. */
public record UpdateLessonRequest(
        @Size(max = 255, message = "title must be 255 characters or fewer")
        String title,

        String summary,

        @Pattern(regexp = "VIDEO|ARTICLE|RESOURCE", message = "lessonType must be VIDEO, ARTICLE, or RESOURCE")
        String lessonType,

        @Min(value = 1, message = "estimatedDurationMinutes must be at least 1")
        Integer estimatedDurationMinutes,

        @Min(value = 1, message = "displayOrder must be at least 1")
        Integer displayOrder,

        Boolean preview,

        String contentUrl,
        String contentBody
) {}

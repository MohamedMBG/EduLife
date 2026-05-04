package com.edulife.courses.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Course detail DTO for the upcoming detail endpoint.
 * It includes nested sections and lessons but still omits internal ownership fields.
 */
public record CourseDetailDto(
        UUID id,
        String slug,
        String title,
        String shortDescription,
        String description,
        String level,
        String languageCode,
        Instant publishedAt,
        List<CourseSectionDto> sections
) {
}

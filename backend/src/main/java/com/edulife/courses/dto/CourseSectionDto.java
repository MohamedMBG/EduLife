package com.edulife.courses.dto;

import java.util.List;
import java.util.UUID;

/**
 * Section DTO for course detail responses.
 * Lessons stay nested here so Android can render the learning outline in one payload.
 */
public record CourseSectionDto(
        UUID id,
        String title,
        String description,
        Integer displayOrder,
        List<LessonSummaryDto> lessons
) {
}

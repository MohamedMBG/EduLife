package com.edulife.courses.dto;

import java.util.UUID;

/**
 * Lightweight lesson shape for course detail responses.
 * It exposes ordering and preview state without leaking any future answer or progress data.
 */
public record LessonSummaryDto(
        UUID id,
        String title,
        String summary,
        String lessonType,
        Integer estimatedDurationMinutes,
        Integer displayOrder,
        boolean preview
) {
}

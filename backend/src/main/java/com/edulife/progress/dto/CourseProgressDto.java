package com.edulife.progress.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Detailed course progress view including per-section and per-lesson completion status.
 */
public record CourseProgressDto(
        UUID courseId,
        int completedLessons,
        int totalLessons,
        double percentComplete,
        List<SectionProgressDto> sections
) {
    /** Progress summary for a single course section. */
    public record SectionProgressDto(
            UUID sectionId,
            String title,
            int displayOrder,
            List<LessonProgressDto> lessons
    ) {}

    /** Completion status for a single lesson within a section. */
    public record LessonProgressDto(
            UUID lessonId,
            String title,
            String lessonType,
            Integer durationMinutes,
            int displayOrder,
            boolean preview,
            boolean completed,
            Instant completedAt
    ) {}
}

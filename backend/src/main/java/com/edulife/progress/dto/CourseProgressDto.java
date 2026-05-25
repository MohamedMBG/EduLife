package com.edulife.progress.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CourseProgressDto(
        UUID courseId,
        int completedLessons,
        int totalLessons,
        double percentComplete,
        List<SectionProgressDto> sections
) {
    public record SectionProgressDto(
            UUID sectionId,
            String title,
            int displayOrder,
            List<LessonProgressDto> lessons
    ) {}

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

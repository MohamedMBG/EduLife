package com.edulife.courses.dto;

import java.util.UUID;

public record LessonDetailDto(
        UUID lessonId,
        UUID courseId,
        UUID sectionId,
        String sectionTitle,
        String title,
        String summary,
        String lessonType,
        String contentUrl,
        String contentBody,
        Integer durationMinutes,
        Integer displayOrder,
        boolean preview,
        boolean completed
) {}

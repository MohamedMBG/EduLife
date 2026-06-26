package com.edulife.courses.dto;

import java.util.UUID;

/** Full lesson detail DTO including content body/URL and the learner's completion status. */
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

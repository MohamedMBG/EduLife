package com.edulife.admin.dto;

import java.time.Instant;
import java.util.UUID;

/** CMS projection of a Lesson row, including contentUrl and contentBody not in LessonSummaryDto. */
public record LessonAdminDto(
        UUID id,
        UUID courseSectionId,
        String title,
        String summary,
        String lessonType,
        Integer estimatedDurationMinutes,
        int displayOrder,
        boolean preview,
        String contentUrl,
        String contentBody,
        Instant createdAt,
        Instant updatedAt
) {}

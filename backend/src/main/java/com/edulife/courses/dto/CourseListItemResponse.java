package com.edulife.courses.dto;

import java.time.Instant;
import java.util.UUID;

public record CourseListItemResponse(
        UUID id,
        String slug,
        String title,
        String shortDescription,
        String level,
        String languageCode,
        Instant publishedAt
) {
}

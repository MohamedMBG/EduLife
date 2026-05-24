package com.edulife.courses.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Summary shape for paginated course discovery responses.
 * It intentionally excludes internal ownership fields and full nested content.
 */
public record CourseSummaryDto(
        UUID id,
        String slug,
        String title,
        String shortDescription,
        String level,
        String languageCode,
        String imageUrl,
        Instant publishedAt
) {
}

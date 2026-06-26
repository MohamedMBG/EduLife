package com.edulife.enrollments.dto;

import java.time.Instant;
import java.util.UUID;

/** Projection of an active enrollment combined with its course metadata. */
public record EnrolledCourseDto(
        UUID enrollmentId,
        UUID courseId,
        String slug,
        String title,
        String shortDescription,
        String level,
        String languageCode,
        String imageUrl,
        Instant enrolledAt
) {}

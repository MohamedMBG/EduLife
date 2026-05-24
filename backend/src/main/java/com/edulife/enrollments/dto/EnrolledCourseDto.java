package com.edulife.enrollments.dto;

import java.time.Instant;
import java.util.UUID;

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

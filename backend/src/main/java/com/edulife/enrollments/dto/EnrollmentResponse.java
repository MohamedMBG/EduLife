package com.edulife.enrollments.dto;

import java.time.Instant;
import java.util.UUID;

/** Response returned after a successful enrollment or reactivation. */
public record EnrollmentResponse(
        UUID enrollmentId,
        UUID courseId,
        Instant enrolledAt,
        String status
) {}

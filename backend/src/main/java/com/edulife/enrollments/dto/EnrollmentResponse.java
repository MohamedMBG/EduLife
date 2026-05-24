package com.edulife.enrollments.dto;

import java.time.Instant;
import java.util.UUID;

public record EnrollmentResponse(
        UUID enrollmentId,
        UUID courseId,
        Instant enrolledAt,
        String status
) {}

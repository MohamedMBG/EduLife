package com.edulife.profiles.dto;

import java.util.UUID;

/** Read-only view of a learner's profile including aggregated enrollment, lesson, and certificate counts. */
public record ProfileDto(
        UUID userId,
        String email,
        String displayName,
        String bio,
        String avatarUrl,
        int enrolledCourses,
        int completedLessons,
        int certificates
) {}

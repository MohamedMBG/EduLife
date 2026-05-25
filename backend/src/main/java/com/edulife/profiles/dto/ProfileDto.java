package com.edulife.profiles.dto;

import java.util.UUID;

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

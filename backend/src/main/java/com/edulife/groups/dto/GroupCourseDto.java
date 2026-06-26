package com.edulife.groups.dto;

import java.time.Instant;
import java.util.UUID;

/** Lightweight DTO for a group-course attachment (IDs and timestamp only). */
public record GroupCourseDto(
        UUID groupId,
        UUID courseId,
        Instant attachedAt
) {}

package com.edulife.groups.dto;

import java.time.Instant;
import java.util.UUID;

/** Attached-course row enriched with course title and status for display. */
public record GroupCourseDetailDto(
        UUID courseId,
        String title,
        String status,
        Instant attachedAt
) {}

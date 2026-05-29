package com.edulife.admin.dto;

import java.time.Instant;
import java.util.UUID;

/** CMS projection of a CourseSection row, including audit timestamps. */
public record SectionAdminDto(
        UUID id,
        UUID courseId,
        String title,
        String description,
        int displayOrder,
        Instant createdAt,
        Instant updatedAt
) {}

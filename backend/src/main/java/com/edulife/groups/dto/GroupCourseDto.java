package com.edulife.groups.dto;

import java.time.Instant;
import java.util.UUID;

public record GroupCourseDto(
        UUID groupId,
        UUID courseId,
        Instant attachedAt
) {}

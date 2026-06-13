package com.edulife.groups.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Full group view: members and attached courses, owner-scoped. */
public record GroupDetailDto(
        UUID id,
        String name,
        Instant createdAt,
        List<GroupMemberDetailDto> members,
        List<GroupCourseDetailDto> courses
) {}

package com.edulife.groups.dto;

import com.edulife.users.model.UserRole;
import java.time.Instant;
import java.util.UUID;

/** Member row enriched with identity fields the group admin needs to recognize people. */
public record GroupMemberDetailDto(
        UUID userId,
        String email,
        UserRole role,
        Instant addedAt
) {}

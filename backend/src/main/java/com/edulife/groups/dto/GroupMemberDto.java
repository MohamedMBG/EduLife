package com.edulife.groups.dto;

import java.time.Instant;
import java.util.UUID;

/** Lightweight DTO for a group membership (IDs and timestamp only). */
public record GroupMemberDto(
        UUID groupId,
        UUID userId,
        Instant addedAt
) {}

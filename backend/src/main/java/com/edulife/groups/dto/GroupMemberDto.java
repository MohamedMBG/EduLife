package com.edulife.groups.dto;

import java.time.Instant;
import java.util.UUID;

public record GroupMemberDto(
        UUID groupId,
        UUID userId,
        Instant addedAt
) {}

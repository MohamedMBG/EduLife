package com.edulife.groups.dto;

import java.time.Instant;
import java.util.UUID;

public record GroupDto(
        UUID id,
        String name,
        UUID createdBy,
        Instant createdAt
) {}

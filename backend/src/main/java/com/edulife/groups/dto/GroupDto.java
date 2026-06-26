package com.edulife.groups.dto;

import java.time.Instant;
import java.util.UUID;

/** Basic group representation returned after creation. */
public record GroupDto(
        UUID id,
        String name,
        UUID createdBy,
        Instant createdAt
) {}

package com.edulife.groups.dto;

import java.time.Instant;
import java.util.UUID;

/** List projection for "my groups" with headline counts for the group dashboard. */
public record GroupSummaryDto(
        UUID id,
        String name,
        Instant createdAt,
        long memberCount,
        long courseCount
) {}

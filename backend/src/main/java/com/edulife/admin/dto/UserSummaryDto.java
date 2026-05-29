package com.edulife.admin.dto;

import com.edulife.users.model.UserRole;
import java.time.Instant;
import java.util.UUID;

/**
 * Read-only projection of a User row returned by admin user-management endpoints.
 * firebaseUid is intentionally omitted — only internal UUIDs cross the API boundary.
 */
public record UserSummaryDto(
        UUID id,
        String email,
        UserRole role,
        Instant createdAt
) {}

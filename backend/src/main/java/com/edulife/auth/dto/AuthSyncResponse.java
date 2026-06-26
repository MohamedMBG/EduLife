package com.edulife.auth.dto;

import com.edulife.users.model.UserRole;
import java.util.UUID;

/** Response returned by POST /api/v1/auth/sync containing the internal user ID and resolved role. */
public record AuthSyncResponse(
        UUID userId,
        UserRole role
) {
}

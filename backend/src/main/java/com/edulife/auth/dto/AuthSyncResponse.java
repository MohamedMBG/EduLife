package com.edulife.auth.dto;

import com.edulife.users.model.UserRole;
import java.util.UUID;

public record AuthSyncResponse(
        UUID userId,
        UserRole role
) {
}

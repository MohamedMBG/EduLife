package com.edulife.admin.dto;

import com.edulife.users.model.UserRole;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for PUT /api/v1/admin/users/{id}/role.
 * Accepts any valid UserRole so the same endpoint handles both promotion and demotion.
 */
public record ChangeRoleRequest(
        @NotNull(message = "role is required")
        UserRole role
) {}

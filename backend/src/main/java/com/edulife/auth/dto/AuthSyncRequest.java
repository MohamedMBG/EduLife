package com.edulife.auth.dto;

import com.edulife.users.model.UserRole;

/**
 * Body for POST /api/v1/auth/sync.
 *
 * <p>{@code intendedRole} is accepted for backward compatibility with existing clients but is
 * NOT trusted: the server always creates self-served accounts as {@code LEARNER}. Privileged
 * roles (TEACHER, GROUP_ADMIN, ADMIN) are assigned only by trusted backend mechanisms (staff
 * allowlist, admin role management) and can never be self-assigned through this field.</p>
 */
public record AuthSyncRequest(
        UserRole intendedRole
) {}

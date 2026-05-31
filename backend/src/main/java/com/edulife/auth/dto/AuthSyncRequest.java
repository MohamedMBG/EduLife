package com.edulife.auth.dto;

import com.edulife.users.model.UserRole;

public record AuthSyncRequest(
        UserRole intendedRole
) {}

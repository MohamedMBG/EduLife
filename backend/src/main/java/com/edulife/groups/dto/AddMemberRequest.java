package com.edulife.groups.dto;

import jakarta.validation.constraints.Email;
import java.util.UUID;

/**
 * Add-member request. Exactly one of userId or email must be provided;
 * email lets group admins add people without knowing internal ids.
 * The service validates the one-of rule.
 */
public record AddMemberRequest(
        UUID userId,

        @Email(message = "email must be a valid address")
        String email
) {}

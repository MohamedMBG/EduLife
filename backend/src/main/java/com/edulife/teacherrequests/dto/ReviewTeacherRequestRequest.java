package com.edulife.teacherrequests.dto;

import jakarta.validation.constraints.Size;

/**
 * Request body for an admin rejecting a teacher request, with an optional note.
 */
public record ReviewTeacherRequestRequest(
        @Size(max = 500, message = "Admin note must be 500 characters or fewer")
        String adminNote
) {}

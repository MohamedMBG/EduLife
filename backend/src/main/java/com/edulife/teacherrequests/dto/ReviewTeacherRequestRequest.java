package com.edulife.teacherrequests.dto;

import jakarta.validation.constraints.Size;

public record ReviewTeacherRequestRequest(
        @Size(max = 500, message = "Admin note must be 500 characters or fewer")
        String adminNote
) {}

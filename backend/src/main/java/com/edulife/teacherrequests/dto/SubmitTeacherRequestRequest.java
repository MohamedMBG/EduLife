package com.edulife.teacherrequests.dto;

import jakarta.validation.constraints.Size;

public record SubmitTeacherRequestRequest(
        @Size(max = 1000, message = "Motivation must be 1000 characters or fewer")
        String motivation
) {}

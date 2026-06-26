package com.edulife.advisor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Inbound request DTO carrying the learner's stated goal for course recommendation. */
public record AdvisorRequest(
        @NotBlank(message = "goal must not be blank")
        @Size(max = 500, message = "goal must not exceed 500 characters")
        String goal
) {}

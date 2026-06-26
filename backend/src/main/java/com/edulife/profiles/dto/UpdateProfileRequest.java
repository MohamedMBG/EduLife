package com.edulife.profiles.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request payload for updating a learner's display name and bio. */
public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) String displayName,
        @Size(max = 500) String bio
) {}

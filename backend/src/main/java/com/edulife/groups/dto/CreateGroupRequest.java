package com.edulife.groups.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request payload for creating a new group. */
public record CreateGroupRequest(
        @NotBlank @Size(max = 255) String name
) {}

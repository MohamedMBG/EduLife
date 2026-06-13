package com.edulife.groups.dto;

import jakarta.validation.constraints.Size;

public record ReviewGroupJoinRequest(
        @Size(max = 1000) String adminNote
) {}

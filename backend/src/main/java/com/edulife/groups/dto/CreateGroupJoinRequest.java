package com.edulife.groups.dto;

import jakarta.validation.constraints.Size;

public record CreateGroupJoinRequest(
        @Size(max = 1000) String motivation
) {}

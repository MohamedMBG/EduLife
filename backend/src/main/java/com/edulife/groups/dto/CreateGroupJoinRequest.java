package com.edulife.groups.dto;

import jakarta.validation.constraints.Size;

/** Request payload for a teacher requesting to join an institute group. */
public record CreateGroupJoinRequest(
        @Size(max = 1000) String motivation
) {}

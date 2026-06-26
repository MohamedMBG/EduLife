package com.edulife.groups.dto;

import jakarta.validation.constraints.Size;

/** Request payload for rejecting a group join request, with an optional admin note. */
public record ReviewGroupJoinRequest(
        @Size(max = 1000) String adminNote
) {}

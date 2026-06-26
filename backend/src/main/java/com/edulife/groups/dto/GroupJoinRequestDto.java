package com.edulife.groups.dto;

import com.edulife.groups.model.GroupJoinRequestStatus;
import java.time.Instant;
import java.util.UUID;

/** Full view of a group join request including requester and reviewer details. */
public record GroupJoinRequestDto(
        UUID id,
        UUID groupId,
        String groupName,
        UUID requesterUserId,
        String requesterEmail,
        GroupJoinRequestStatus status,
        String motivation,
        String adminNote,
        UUID reviewedByUserId,
        String reviewedByEmail,
        Instant requestedAt,
        Instant reviewedAt
) {}

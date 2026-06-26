package com.edulife.teacherrequests.dto;

import com.edulife.teacherrequests.entity.TeacherRequest;
import com.edulife.teacherrequests.model.RequestStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * API response DTO representing a teacher promotion request and its review status.
 */
public record TeacherRequestResponse(
        UUID id,
        UUID userId,
        String userEmail,
        RequestStatus status,
        String motivation,
        String adminNote,
        Instant requestedAt,
        Instant reviewedAt
) {
    public static TeacherRequestResponse from(TeacherRequest r) {
        return new TeacherRequestResponse(
                r.getId(),
                r.getUser().getId(),
                r.getUser().getEmail(),
                r.getStatus(),
                r.getMotivation(),
                r.getAdminNote(),
                r.getRequestedAt(),
                r.getReviewedAt()
        );
    }
}

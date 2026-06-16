package com.edulife.certificates.dto;

import java.time.Instant;
import java.util.UUID;

public record CertificateSummaryDto(
        UUID id,
        UUID courseId,
        String certificateNumber,
        String learnerName,
        String teacherName,
        String courseTitle,
        String courseLevel,
        Instant issuedAt,
        String verificationHash
) {}

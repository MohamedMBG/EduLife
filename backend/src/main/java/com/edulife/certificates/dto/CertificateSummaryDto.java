package com.edulife.certificates.dto;

import java.time.Instant;
import java.util.UUID;

public record CertificateSummaryDto(
        UUID id,
        UUID courseId,
        String certificateNumber,
        String courseTitle,
        Instant issuedAt
) {}

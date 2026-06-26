package com.edulife.certificates.dto;

import java.time.Instant;
import java.util.UUID;

/** Summary DTO for certificate listings, including snapshot data but excluding the PDF URL. */
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

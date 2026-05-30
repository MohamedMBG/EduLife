package com.edulife.certificates.dto;

import java.time.Instant;
import java.util.UUID;

public record CertificateDetailDto(
        UUID id,
        UUID courseId,
        String certificateNumber,
        String studentName,
        String courseTitle,
        String issuerName,
        Instant issuedAt,
        String verificationHash,
        String pdfUrl
) {}

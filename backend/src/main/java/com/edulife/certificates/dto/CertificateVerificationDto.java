package com.edulife.certificates.dto;

import java.time.Instant;

public record CertificateVerificationDto(
        String studentName,
        String courseTitle,
        String issuerName,
        Instant issuedAt,
        String certificateNumber,
        boolean valid
) {}

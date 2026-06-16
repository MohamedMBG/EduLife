package com.edulife.certificates.dto;

import java.time.Instant;

public record CertificateVerificationDto(
        String learnerName,
        String teacherName,
        String courseTitle,
        String courseLevel,
        Instant issuedAt,
        String certificateNumber,
        String verificationHash,
        boolean valid
) {}

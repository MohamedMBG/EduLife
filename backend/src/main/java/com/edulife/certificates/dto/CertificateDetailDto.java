package com.edulife.certificates.dto;

import java.time.Instant;
import java.util.UUID;

public record CertificateDetailDto(
        UUID id,
        UUID courseId,
        String certificateNumber,
        String learnerName,
        String teacherName,
        String courseTitle,
        String courseLevel,
        Instant issuedAt,
        String verificationHash,
        String pdfUrl
) {}

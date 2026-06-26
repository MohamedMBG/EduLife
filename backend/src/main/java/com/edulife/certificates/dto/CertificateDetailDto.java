package com.edulife.certificates.dto;

import java.time.Instant;
import java.util.UUID;

/** Full certificate details including learner/teacher names, course info, verification hash, and PDF URL. */
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

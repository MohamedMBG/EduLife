package com.edulife.certificates.dto;

import java.time.Instant;
import java.util.UUID;

/** Lightweight certificate DTO with core identifiers and issue timestamp. */
public record CertificateDto(
        UUID certificateId,
        UUID courseId,
        String certificateNumber,
        Instant issuedAt
) {}

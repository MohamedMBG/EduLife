package com.edulife.certificates.dto;

import java.time.Instant;
import java.util.UUID;

public record CertificateDto(
        UUID certificateId,
        UUID courseId,
        String certificateNumber,
        Instant issuedAt
) {}

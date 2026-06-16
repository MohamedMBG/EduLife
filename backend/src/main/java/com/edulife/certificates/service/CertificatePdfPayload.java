package com.edulife.certificates.service;

import com.edulife.certificates.entity.Certificate;
import java.time.Instant;

/**
 * Immutable render model for certificate PDFs.
 *
 * <p>The certificate service resolves missing learner, teacher, and course data before rendering so
 * historical rows do not leak placeholder names into downloaded credentials.</p>
 */
public record CertificatePdfPayload(
        String learnerName,
        String teacherName,
        String courseTitle,
        String courseLevel,
        Instant issuedAt,
        String certificateNumber,
        String verificationHash
) {

    static CertificatePdfPayload fromCertificate(Certificate cert) {
        return new CertificatePdfPayload(
                cert.getLearnerNameSnapshot(),
                cert.getTeacherNameSnapshot(),
                cert.getCourseTitleSnapshot(),
                cert.getCourseLevelSnapshot(),
                cert.getIssuedAt(),
                cert.getCertificateNumber(),
                cert.getVerificationHash()
        );
    }
}

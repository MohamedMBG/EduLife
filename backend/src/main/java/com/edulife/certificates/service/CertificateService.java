package com.edulife.certificates.service;

import com.edulife.certificates.config.CertificateStorageProperties;
import com.edulife.certificates.dto.CertificateDetailDto;
import com.edulife.certificates.dto.CertificateSummaryDto;
import com.edulife.certificates.dto.CertificateVerificationDto;
import com.edulife.certificates.entity.Certificate;
import com.edulife.certificates.exception.CertificateAccessDeniedException;
import com.edulife.certificates.exception.CertificateGenerationException;
import com.edulife.certificates.exception.CertificateNotDownloadableException;
import com.edulife.certificates.exception.CertificateNotFoundException;
import com.edulife.certificates.repository.CertificateRepository;
import com.edulife.courses.entity.Course;
import com.edulife.courses.repository.CourseRepository;
import com.edulife.profiles.entity.Profile;
import com.edulife.profiles.repository.ProfileRepository;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Year;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

@Service
@EnableConfigurationProperties(CertificateStorageProperties.class)
public class CertificateService {

    private static final Logger log = LoggerFactory.getLogger(CertificateService.class);

    private final CertificateRepository certificateRepository;
    private final ProfileRepository profileRepository;
    private final CourseRepository courseRepository;
    private final CertificatePdfService pdfService;
    private final CertificateStorageProperties storageProperties;

    public CertificateService(
            CertificateRepository certificateRepository,
            ProfileRepository profileRepository,
            CourseRepository courseRepository,
            CertificatePdfService pdfService,
            CertificateStorageProperties storageProperties) {
        this.certificateRepository = certificateRepository;
        this.profileRepository = profileRepository;
        this.courseRepository = courseRepository;
        this.pdfService = pdfService;
        this.storageProperties = storageProperties;
    }

    public CertificateDetailDto generateCertificateAfterExamPass(UUID userId, UUID courseId, UUID examAttemptId) {
        if (certificateRepository.existsByUserIdAndCourseId(userId, courseId)) {
            Certificate existing = certificateRepository.findByUserIdAndCourseId(userId, courseId)
                    .orElseThrow(() -> new CertificateNotFoundException("Certificate not found"));
            return toDetailDto(existing);
        }

        String learnerName = resolveUserDisplayName(userId, "learner");
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CertificateGenerationException("Course not found for certificate generation"));
        if (course.getCreatedByUserId() == null) {
            throw new CertificateGenerationException("Course is missing an instructor for certificate generation");
        }
        String teacherName = resolveUserDisplayName(course.getCreatedByUserId(), "teacher");
        String courseTitle = requireText(course.getTitle(), "Course title is missing for certificate generation");
        String courseLevel = requireText(course.getLevel(), "Course level is missing for certificate generation");

        String certificateNumber = generateCertificateNumber();
        String verificationHash = generateVerificationHash(certificateNumber, userId, courseId);

        // Persist the certificate first so the snapshot (and the @PrePersist issuedAt) is the single
        // source of truth the PDF renders from — both at issue time and on every later download.
        Certificate cert = new Certificate(userId, courseId, examAttemptId, certificateNumber,
                learnerName, teacherName, courseTitle, courseLevel, verificationHash, null);
        cert = certificateRepository.save(cert);

        // Pre-render and cache a copy on disk for convenience/audit. This is best-effort: the
        // download endpoint regenerates from the snapshot, so a storage hiccup must never fail
        // issuance (which is tied to the authoritative exam-pass result).
        try {
            byte[] pdfBytes = pdfService.generatePdf(cert);
            Path pdfPath = savePdf(cert.getId(), pdfBytes);
            cert.setPdfUrl(pdfPath.toString());
            cert = certificateRepository.save(cert);
        } catch (Exception e) {
            log.warn("Could not pre-render certificate PDF for {}; it will be generated on download.",
                    cert.getId(), e);
        }

        return toDetailDto(cert);
    }

    public List<CertificateSummaryDto> getMyCertificates(UUID userId) {
        return certificateRepository.findAllByUserId(userId).stream()
                .map(this::toSummaryDto)
                .toList();
    }

    public CertificateDetailDto getCertificateById(UUID userId, UUID certificateId) {
        Certificate cert = certificateRepository.findByIdAndUserId(certificateId, userId)
                .orElseThrow(() -> new CertificateNotFoundException("Certificate not found"));
        return toDetailDto(cert);
    }

    public CertificateVerificationDto verifyCertificate(String verificationHash) {
        Certificate cert = certificateRepository.findByVerificationHash(verificationHash)
                .orElseThrow(() -> new CertificateNotFoundException("Certificate not found for the given verification hash"));
        return new CertificateVerificationDto(
                cert.getLearnerNameSnapshot(),
                cert.getTeacherNameSnapshot(),
                cert.getCourseTitleSnapshot(),
                cert.getCourseLevelSnapshot(),
                cert.getIssuedAt(),
                cert.getCertificateNumber(),
                cert.getVerificationHash(),
                true
        );
    }

    public byte[] getCertificatePdfForDownload(UUID userId, UUID certificateId) {
        // Look up by id first so we can distinguish "missing" (404) from "owned by another learner"
        // (403) instead of collapsing both into a not-found.
        Certificate cert = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new CertificateNotFoundException("Certificate not found"));
        if (!userId.equals(cert.getUserId())) {
            throw new CertificateAccessDeniedException("This certificate belongs to another learner");
        }
        if (cert.getVerificationHash() == null || cert.getVerificationHash().isBlank()) {
            throw new CertificateNotDownloadableException("Certificate is not eligible for download");
        }

        // Regenerate from the snapshot rather than reading a possibly-missing stored file. This keeps
        // the verification hash identical to the one shown in the UI (it is read, never recomputed).
        try {
            return pdfService.generatePdf(cert);
        } catch (Exception e) {
            throw new CertificateGenerationException("Failed to render certificate PDF", e);
        }
    }

    private String generateCertificateNumber() {
        String year = String.valueOf(Year.now().getValue());
        String unique = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return "EL-" + year + "-" + unique;
    }

    private String resolveUserDisplayName(UUID userId, String roleDescription) {
        return profileRepository.findByUserId(userId)
                .map(Profile::getDisplayName)
                .filter(name -> name != null && !name.isBlank())
                .orElseThrow(() -> new CertificateGenerationException(
                        "Missing " + roleDescription + " full name for certificate generation"));
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new CertificateGenerationException(message);
        }
        return value;
    }

    private String generateVerificationHash(String certificateNumber, UUID userId, UUID courseId) {
        try {
            String raw = certificateNumber + ":" + userId + ":" + courseId;
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception e) {
            throw new CertificateGenerationException("Failed to generate verification hash", e);
        }
    }

    private Path savePdf(UUID certificateId, byte[] pdfBytes) throws Exception {
        Path dir = Path.of(storageProperties.getStorageDir());
        Files.createDirectories(dir);
        Path file = dir.resolve("certificate-" + certificateId + ".pdf");
        Files.write(file, pdfBytes);
        return file;
    }

    private CertificateSummaryDto toSummaryDto(Certificate cert) {
        return new CertificateSummaryDto(
                cert.getId(),
                cert.getCourseId(),
                cert.getCertificateNumber(),
                cert.getLearnerNameSnapshot(),
                cert.getTeacherNameSnapshot(),
                cert.getCourseTitleSnapshot(),
                cert.getCourseLevelSnapshot(),
                cert.getIssuedAt(),
                cert.getVerificationHash()
        );
    }

    private CertificateDetailDto toDetailDto(Certificate cert) {
        return new CertificateDetailDto(
                cert.getId(),
                cert.getCourseId(),
                cert.getCertificateNumber(),
                cert.getLearnerNameSnapshot(),
                cert.getTeacherNameSnapshot(),
                cert.getCourseTitleSnapshot(),
                cert.getCourseLevelSnapshot(),
                cert.getIssuedAt(),
                cert.getVerificationHash(),
                cert.getPdfUrl()
        );
    }
}

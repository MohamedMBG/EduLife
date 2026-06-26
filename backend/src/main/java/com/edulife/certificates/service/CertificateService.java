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
import com.edulife.users.entity.User;
import com.edulife.users.repository.UserRepository;
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

/**
 * Core service for certificate lifecycle: generation after exam pass, retrieval, PDF download, and verification.
 *
 * <p>Certificates are only issued server-side after a passing exam attempt. Point-in-time snapshots of
 * learner, teacher, and course data are stored so the credential remains accurate regardless of future
 * profile or course edits. Historical rows missing snapshot data are resolved on read from live sources.</p>
 */
@Service
@EnableConfigurationProperties(CertificateStorageProperties.class)
public class CertificateService {

    private static final Logger log = LoggerFactory.getLogger(CertificateService.class);

    private final CertificateRepository certificateRepository;
    private final ProfileRepository profileRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CertificatePdfService pdfService;
    private final CertificateStorageProperties storageProperties;

    public CertificateService(
            CertificateRepository certificateRepository,
            ProfileRepository profileRepository,
            CourseRepository courseRepository,
            UserRepository userRepository,
            CertificatePdfService pdfService,
            CertificateStorageProperties storageProperties) {
        this.certificateRepository = certificateRepository;
        this.profileRepository = profileRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.pdfService = pdfService;
        this.storageProperties = storageProperties;
    }

    /**
     * Generates a certificate after a successful exam pass, or returns an existing one if already issued.
     * Snapshots learner/teacher/course data, persists the certificate, and best-effort pre-renders the PDF.
     */
    public CertificateDetailDto generateCertificateAfterExamPass(UUID userId, UUID courseId, UUID examAttemptId) {
        if (certificateRepository.existsByUserIdAndCourseId(userId, courseId)) {
            Certificate existing = certificateRepository.findByUserIdAndCourseId(userId, courseId)
                    .orElseThrow(() -> new CertificateNotFoundException("Certificate not found"));
            return toDetailDto(existing);
        }

        String learnerName = resolveLearnerName(userId);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CertificateGenerationException("Course not found for certificate generation"));
        String teacherName = resolveTeacherName(course);
        String courseTitle = fallbackText(course.getTitle(), "EduLife Course");
        String courseLevel = fallbackText(course.getLevel(), "All Levels");

        String certificateNumber = generateCertificateNumber();
        String verificationHash = generateVerificationHash(certificateNumber, userId, courseId);

        // Persist the certificate first so the snapshot (and the @PrePersist issuedAt) is the single
        // source of truth the PDF renders from at issue time and on every later download.
        Certificate cert = new Certificate(userId, courseId, examAttemptId, certificateNumber,
                learnerName, teacherName, courseTitle, courseLevel, verificationHash, null);
        cert = certificateRepository.save(cert);

        // Pre-render and cache a copy on disk for convenience/audit. This is best-effort: the
        // download endpoint regenerates from the snapshot, so a storage hiccup must never fail
        // issuance (which is tied to the authoritative exam-pass result).
        try {
            byte[] pdfBytes = pdfService.generatePdf(toPdfPayload(cert));
            Path pdfPath = savePdf(cert.getId(), pdfBytes);
            cert.setPdfUrl(pdfPath.toString());
            cert = certificateRepository.save(cert);
        } catch (Exception e) {
            log.warn("Could not pre-render certificate PDF for {}; it will be generated on download.",
                    cert.getId(), e);
        }

        return toDetailDto(cert);
    }

    /** Returns all certificates for a learner as summary DTOs. */
    public List<CertificateSummaryDto> getMyCertificates(UUID userId) {
        return certificateRepository.findAllByUserId(userId).stream()
                .map(this::toSummaryDto)
                .toList();
    }

    /** Returns a single certificate's details; only the owning learner may access it. */
    public CertificateDetailDto getCertificateById(UUID userId, UUID certificateId) {
        Certificate cert = certificateRepository.findByIdAndUserId(certificateId, userId)
                .orElseThrow(() -> new CertificateNotFoundException("Certificate not found"));
        return toDetailDto(cert);
    }

    /** Verifies a certificate's authenticity by its SHA-256 verification hash (public, no auth required). */
    public CertificateVerificationDto verifyCertificate(String verificationHash) {
        Certificate cert = certificateRepository.findByVerificationHash(verificationHash)
                .orElseThrow(() -> new CertificateNotFoundException("Certificate not found for the given verification hash"));
        ResolvedCertificateData data = resolveCertificateData(cert);
        return new CertificateVerificationDto(
                data.learnerName(),
                data.teacherName(),
                data.courseTitle(),
                data.courseLevel(),
                cert.getIssuedAt(),
                cert.getCertificateNumber(),
                cert.getVerificationHash(),
                true
        );
    }

    /**
     * Regenerates and returns the PDF bytes for download, verifying ownership and downloadability.
     * The PDF is rendered on demand from the snapshot rather than reading a stored file.
     */
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
            return pdfService.generatePdf(toPdfPayload(cert));
        } catch (Exception e) {
            throw new CertificateGenerationException("Failed to render certificate PDF", e);
        }
    }

    private String generateCertificateNumber() {
        String year = String.valueOf(Year.now().getValue());
        String unique = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return "EL-" + year + "-" + unique;
    }

    private String resolveLearnerName(UUID userId) {
        return resolveUserDisplayName(userId, "Learner " + shortUuid(userId));
    }

    private String resolveTeacherName(Course course) {
        if (course.getCreatedByUserId() == null) {
            return "EduLife Instructor";
        }
        return resolveUserDisplayName(course.getCreatedByUserId(), "EduLife Instructor");
    }

    /** Resolves a display name from profile, then email, falling back to the provided default. */
    private String resolveUserDisplayName(UUID userId, String fallbackName) {
        String profileName = profileRepository.findByUserId(userId)
                .map(Profile::getDisplayName)
                .filter(name -> name != null && !name.isBlank())
                .filter(name -> !"DELETED_USER".equalsIgnoreCase(name.trim()))
                .orElse(null);
        if (profileName != null) {
            return profileName;
        }
        return userRepository.findById(userId)
                .map(User::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .map(this::readableNameFromEmail)
                .orElse(fallbackName);
    }

    private String readableNameFromEmail(String email) {
        String localPart = email.split("@", 2)[0].replaceAll("[._-]+", " ").trim();
        if (localPart.isBlank()) {
            return email;
        }
        StringBuilder readable = new StringBuilder();
        for (String part : localPart.split("\\s+")) {
            if (!part.isBlank()) {
                readable.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1).toLowerCase())
                        .append(' ');
            }
        }
        return readable.toString().trim();
    }

    private String fallbackText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /** Generates a SHA-256 verification hash from the certificate number, user ID, and course ID. */
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
        ResolvedCertificateData data = resolveCertificateData(cert);
        return new CertificateSummaryDto(
                cert.getId(),
                cert.getCourseId(),
                cert.getCertificateNumber(),
                data.learnerName(),
                data.teacherName(),
                data.courseTitle(),
                data.courseLevel(),
                cert.getIssuedAt(),
                cert.getVerificationHash()
        );
    }

    private CertificateDetailDto toDetailDto(Certificate cert) {
        ResolvedCertificateData data = resolveCertificateData(cert);
        return new CertificateDetailDto(
                cert.getId(),
                cert.getCourseId(),
                cert.getCertificateNumber(),
                data.learnerName(),
                data.teacherName(),
                data.courseTitle(),
                data.courseLevel(),
                cert.getIssuedAt(),
                cert.getVerificationHash(),
                cert.getPdfUrl()
        );
    }

    private CertificatePdfPayload toPdfPayload(Certificate cert) {
        ResolvedCertificateData data = resolveCertificateData(cert);
        return new CertificatePdfPayload(
                data.learnerName(),
                data.teacherName(),
                data.courseTitle(),
                data.courseLevel(),
                cert.getIssuedAt(),
                cert.getCertificateNumber(),
                cert.getVerificationHash()
        );
    }

    /** Resolves certificate display data, filling missing snapshots from live sources for historical rows. */
    private ResolvedCertificateData resolveCertificateData(Certificate cert) {
        Course course = null;
        if (hasMissingCourseOrTeacherSnapshot(cert)) {
            course = courseRepository.findById(cert.getCourseId()).orElse(null);
        }
        // Historical rows may predate snapshot columns. Resolve missing values for display/PDF
        // without touching the verification hash or trusting client-provided identity values.
        String learnerName = isBlank(cert.getLearnerNameSnapshot())
                ? resolveLearnerName(cert.getUserId())
                : cert.getLearnerNameSnapshot();
        String teacherName = isBlank(cert.getTeacherNameSnapshot())
                ? (course == null ? "EduLife Instructor" : resolveTeacherName(course))
                : cert.getTeacherNameSnapshot();
        String courseTitle = isBlank(cert.getCourseTitleSnapshot())
                ? (course == null ? "EduLife Course" : fallbackText(course.getTitle(), "EduLife Course"))
                : cert.getCourseTitleSnapshot();
        String courseLevel = isBlank(cert.getCourseLevelSnapshot())
                ? (course == null ? "All Levels" : fallbackText(course.getLevel(), "All Levels"))
                : cert.getCourseLevelSnapshot();
        return new ResolvedCertificateData(learnerName, teacherName, courseTitle, courseLevel);
    }

    private boolean hasMissingCourseOrTeacherSnapshot(Certificate cert) {
        return isBlank(cert.getTeacherNameSnapshot())
                || isBlank(cert.getCourseTitleSnapshot())
                || isBlank(cert.getCourseLevelSnapshot());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String shortUuid(UUID id) {
        return id.toString().substring(0, 8);
    }

    /** Internal record holding resolved display names and course info for a certificate. */
    private record ResolvedCertificateData(
            String learnerName,
            String teacherName,
            String courseTitle,
            String courseLevel
    ) {}
}

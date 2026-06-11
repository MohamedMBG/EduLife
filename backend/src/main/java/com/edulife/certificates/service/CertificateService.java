package com.edulife.certificates.service;

import com.edulife.certificates.config.CertificateStorageProperties;
import com.edulife.certificates.dto.CertificateDetailDto;
import com.edulife.certificates.dto.CertificateSummaryDto;
import com.edulife.certificates.dto.CertificateVerificationDto;
import com.edulife.certificates.entity.Certificate;
import com.edulife.certificates.exception.CertificateGenerationException;
import com.edulife.certificates.exception.CertificateNotFoundException;
import com.edulife.certificates.repository.CertificateRepository;
import com.edulife.courses.entity.Course;
import com.edulife.courses.repository.CourseRepository;
import com.edulife.profiles.entity.Profile;
import com.edulife.profiles.repository.ProfileRepository;
import com.edulife.users.repository.UserRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.Year;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
@EnableConfigurationProperties(CertificateStorageProperties.class)
public class CertificateService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM d, yyyy").withZone(ZoneOffset.UTC);

    private final CertificateRepository certificateRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final CourseRepository courseRepository;
    private final SpringTemplateEngine templateEngine;
    private final CertificateStorageProperties storageProperties;

    public CertificateService(
            CertificateRepository certificateRepository,
            UserRepository userRepository,
            ProfileRepository profileRepository,
            CourseRepository courseRepository,
            SpringTemplateEngine templateEngine,
            CertificateStorageProperties storageProperties) {
        this.certificateRepository = certificateRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.courseRepository = courseRepository;
        this.templateEngine = templateEngine;
        this.storageProperties = storageProperties;
    }

    public CertificateDetailDto generateCertificateAfterExamPass(UUID userId, UUID courseId, UUID examAttemptId) {
        if (certificateRepository.existsByUserIdAndCourseId(userId, courseId)) {
            Certificate existing = certificateRepository.findByUserIdAndCourseId(userId, courseId)
                    .orElseThrow(() -> new CertificateNotFoundException("Certificate not found"));
            return toDetailDto(existing);
        }

        String studentName = profileRepository.findByUserId(userId)
                .map(Profile::getDisplayName)
                .filter(name -> name != null && !name.isBlank())
                .orElse("EduLife Learner");

        Course course = courseRepository.findById(courseId).orElse(null);
        String courseTitle = course != null ? course.getTitle() : "Course";

        String issuerName = "EduLife";
        if (course != null && course.getCreatedByUserId() != null) {
            issuerName = profileRepository.findByUserId(course.getCreatedByUserId())
                    .map(Profile::getDisplayName)
                    .filter(name -> name != null && !name.isBlank())
                    .orElse("EduLife");
        }

        String certificateNumber = generateCertificateNumber();
        String verificationHash = generateVerificationHash(certificateNumber, userId, courseId);

        try {
            String verifyUrl = storageProperties.getPublicBaseUrl() + "/verify/" + verificationHash;
            String qrCodeDataUri = generateQrCodeDataUri(verifyUrl);

            Map<String, Object> templateVars = Map.of(
                    "studentName", studentName,
                    "courseTitle", courseTitle,
                    "issuerName", issuerName,
                    "issuedAt", DATE_FORMATTER.format(Instant.now()),
                    "certificateNumber", certificateNumber,
                    "verificationCode", verificationHash.substring(0, 16) + "...",
                    "qrCodeDataUri", qrCodeDataUri
            );

            String html = renderCertificateHtml(templateVars);
            byte[] pdfBytes = htmlToPdf(html);

            Certificate cert = new Certificate(userId, courseId, examAttemptId, certificateNumber,
                    studentName, courseTitle, issuerName, verificationHash, null);
            cert = certificateRepository.save(cert);

            Path pdfPath = savePdf(cert.getId(), pdfBytes);
            cert.setPdfUrl(pdfPath.toString());
            cert = certificateRepository.save(cert);

            return toDetailDto(cert);
        } catch (Exception e) {
            throw new CertificateGenerationException("Failed to generate certificate PDF", e);
        }
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
                cert.getStudentName(),
                cert.getCourseTitle(),
                cert.getIssuerName(),
                cert.getIssuedAt(),
                cert.getCertificateNumber(),
                true
        );
    }

    public byte[] getCertificatePdfForDownload(UUID userId, UUID certificateId) {
        Certificate cert = certificateRepository.findByIdAndUserId(certificateId, userId)
                .orElseThrow(() -> new CertificateNotFoundException("Certificate not found"));

        String pdfUrl = cert.getPdfUrl();
        if (pdfUrl == null || pdfUrl.isBlank()) {
            throw new CertificateNotFoundException("PDF not available for this certificate");
        }

        try {
            Path storageRoot = Path.of(storageProperties.getStorageDir()).toAbsolutePath().normalize();
            Path target = Path.of(pdfUrl).toAbsolutePath().normalize();
            if (!target.startsWith(storageRoot)) {
                throw new CertificateNotFoundException("PDF not available for this certificate");
            }
            return Files.readAllBytes(target);
        } catch (CertificateNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new CertificateGenerationException("Could not read certificate PDF from storage", e);
        }
    }

    private String generateCertificateNumber() {
        String year = String.valueOf(Year.now().getValue());
        String unique = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return "EL-" + year + "-" + unique;
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

    private String generateQrCodeDataUri(String content) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, 200, 200);
        ByteArrayOutputStream pngOut = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", pngOut);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(pngOut.toByteArray());
    }

    private String renderCertificateHtml(Map<String, Object> vars) {
        Context ctx = new Context();
        ctx.setVariables(vars);
        return templateEngine.process("certificate-academic", ctx);
    }

    private byte[] htmlToPdf(String html) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
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
                cert.getCourseTitle(),
                cert.getIssuedAt()
        );
    }

    private CertificateDetailDto toDetailDto(Certificate cert) {
        return new CertificateDetailDto(
                cert.getId(),
                cert.getCourseId(),
                cert.getCertificateNumber(),
                cert.getStudentName(),
                cert.getCourseTitle(),
                cert.getIssuerName(),
                cert.getIssuedAt(),
                cert.getVerificationHash(),
                cert.getPdfUrl()
        );
    }
}

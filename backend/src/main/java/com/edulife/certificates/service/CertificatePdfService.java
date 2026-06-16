package com.edulife.certificates.service;

import com.edulife.certificates.config.CertificateStorageProperties;
import com.edulife.certificates.entity.Certificate;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * Renders the certificate PDF from a persisted {@link Certificate} snapshot.
 *
 * <p>Rendering is deterministic and stateless: it depends only on the certificate row (snapshots +
 * verification hash) and never on a previously written file. This lets the download endpoint
 * regenerate the PDF on demand so a missing or relocated storage file can no longer surface as a
 * "Certificate generation failed" error to the learner.</p>
 */
@Service
@EnableConfigurationProperties(CertificateStorageProperties.class)
public class CertificatePdfService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM d, yyyy").withZone(ZoneOffset.UTC);

    private final SpringTemplateEngine templateEngine;
    private final CertificateStorageProperties storageProperties;

    public CertificatePdfService(
            SpringTemplateEngine templateEngine,
            CertificateStorageProperties storageProperties) {
        this.templateEngine = templateEngine;
        this.storageProperties = storageProperties;
    }

    /**
     * Builds the PDF bytes for an issued certificate. Snapshot fields fall back to safe defaults so
     * stale or partially populated rows never crash PDF generation, while the verification hash and
     * certificate number are always taken verbatim from the entity to keep the credential authentic.
     */
    public byte[] generatePdf(Certificate cert) throws Exception {
        return generatePdf(CertificatePdfPayload.fromCertificate(cert));
    }

    /**
     * Builds PDF bytes from a resolved render payload. The verification hash is read as-is and is
     * never recomputed here, preserving the certificate's original public verification identity.
     */
    public byte[] generatePdf(CertificatePdfPayload payload) throws Exception {
        String verificationHash = safeText(payload.verificationHash());
        String verifyUrl = storageProperties.getPublicBaseUrl() + "/verify/" + verificationHash;
        Instant issuedAt = payload.issuedAt() != null ? payload.issuedAt() : Instant.now();

        Map<String, Object> templateVars = new HashMap<>();
        templateVars.put("learnerName", fallback(payload.learnerName(), "Learner"));
        templateVars.put("courseTitle", fallback(payload.courseTitle(), "EduLife Course"));
        templateVars.put("courseLevel", fallback(payload.courseLevel(), "All Levels"));
        templateVars.put("teacherName", fallback(payload.teacherName(), "EduLife Instructor"));
        templateVars.put("issuedAt", formatCertificateDate(issuedAt));
        templateVars.put("certificateNumber", fallback(payload.certificateNumber(), "EduLife Certificate"));
        templateVars.put("verificationCode", shortCode(verificationHash));
        templateVars.put("verificationHash", verificationHash);
        // The QR image points at the public verification URL; it carries no private internal IDs.
        templateVars.put("qrCodeDataUri", generateQrCodeDataUri(verifyUrl));

        String html = renderCertificateHtml(templateVars);
        return htmlToPdf(html);
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String fallback(String value, String defaultValue) {
        return value != null && !value.isBlank() ? value : defaultValue;
    }

    private String shortCode(String verificationHash) {
        if (verificationHash.length() <= 16) {
            return verificationHash;
        }
        return verificationHash.substring(0, 16) + "...";
    }

    private String formatCertificateDate(Instant issuedAt) {
        return DATE_FORMATTER.format(issuedAt);
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
}

package com.edulife.certificates;

import com.edulife.certificates.config.CertificateStorageProperties;
import com.edulife.certificates.entity.Certificate;
import com.edulife.certificates.service.CertificatePdfService;
import java.time.Instant;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import static org.assertj.core.api.Assertions.assertThat;

class CertificatePdfServiceTest {

    private static final UUID LEARNER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID COURSE_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID ATTEMPT_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    // 64-char hex, mirrors the SHA-256 verification hash format stored on real certificates.
    private static final String HASH = "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";

    private CertificatePdfService pdfService() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        CertificateStorageProperties properties = new CertificateStorageProperties();
        properties.setPublicBaseUrl("http://localhost:8080/api/v1/certificates");
        return new CertificatePdfService(engine, properties);
    }

    private Certificate certificate() {
        Certificate cert = new Certificate(LEARNER_ID, COURSE_ID, ATTEMPT_ID, "EL-2026-ABC123DEF456",
                "Jane Learner", "Prof. Teacher", "Algebra Foundations", "BEGINNER", HASH, null);
        ReflectionTestUtils.setField(cert, "issuedAt", Instant.parse("2026-06-16T10:00:00Z"));
        return cert;
    }

    @Test
    void generatePdfProducesNonEmptyPdfBytes() throws Exception {
        byte[] pdf = pdfService().generatePdf(certificate());

        assertThat(pdf).isNotEmpty();
        // PDF magic number "%PDF".
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void generatePdfContainsAllCredentialFields() throws Exception {
        byte[] pdf = pdfService().generatePdf(certificate());

        String text;
        try (PDDocument document = PDDocument.load(pdf)) {
            text = new PDFTextStripper().getText(document);
        }

        assertThat(text).contains("Jane Learner");
        assertThat(text).contains("Prof. Teacher");
        assertThat(text).contains("Algebra Foundations");
        assertThat(text).contains("BEGINNER");
        assertThat(text).contains("EL-2026-ABC123DEF456");
        assertThat(text).contains("June 16, 2026");
        // The certificate prints the leading 16 chars of the verification hash as the code.
        assertThat(text).contains(HASH.substring(0, 16));
    }

    @Test
    void generatePdfFallsBackForMissingSnapshotFields() throws Exception {
        Certificate cert = new Certificate(LEARNER_ID, COURSE_ID, ATTEMPT_ID, "EL-2026-FALLBACK",
                null, null, null, null, HASH, null);
        ReflectionTestUtils.setField(cert, "issuedAt", Instant.parse("2026-06-16T10:00:00Z"));

        byte[] pdf = pdfService().generatePdf(cert);

        String text;
        try (PDDocument document = PDDocument.load(pdf)) {
            text = new PDFTextStripper().getText(document);
        }

        assertThat(text).contains("EduLife Learner");
        assertThat(text).contains("EduLife Instructor");
        assertThat(text).contains("EduLife Course");
        assertThat(text).contains("All Levels");
    }
}

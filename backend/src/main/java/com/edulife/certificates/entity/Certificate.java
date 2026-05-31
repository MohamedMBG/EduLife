package com.edulife.certificates.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "certificates")
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "course_id", nullable = false, updatable = false)
    private UUID courseId;

    @Column(name = "exam_attempt_id", updatable = false)
    private UUID examAttemptId;

    @Column(name = "certificate_number", nullable = false, unique = true, length = 50)
    private String certificateNumber;

    @Column(name = "student_name", length = 200)
    private String studentName;

    @Column(name = "course_title", length = 255)
    private String courseTitle;

    @Column(name = "issuer_name", length = 200)
    private String issuerName;

    @Column(name = "verification_hash", length = 128, unique = true)
    private String verificationHash;

    @Column(name = "pdf_url")
    private String pdfUrl;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Certificate() {}

    public Certificate(UUID userId, UUID courseId, String certificateNumber) {
        this.userId = userId;
        this.courseId = courseId;
        this.certificateNumber = certificateNumber;
    }

    public Certificate(UUID userId, UUID courseId, UUID examAttemptId, String certificateNumber,
                       String studentName, String courseTitle, String issuerName,
                       String verificationHash, String pdfUrl) {
        this.userId = userId;
        this.courseId = courseId;
        this.examAttemptId = examAttemptId;
        this.certificateNumber = certificateNumber;
        this.studentName = studentName;
        this.courseTitle = courseTitle;
        this.issuerName = issuerName;
        this.verificationHash = verificationHash;
        this.pdfUrl = pdfUrl;
    }

    @PrePersist
    void onCreate() {
        issuedAt = Instant.now();
        createdAt = issuedAt;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getCourseId() { return courseId; }
    public UUID getExamAttemptId() { return examAttemptId; }
    public String getCertificateNumber() { return certificateNumber; }
    public String getStudentName() { return studentName; }
    public String getCourseTitle() { return courseTitle; }
    public String getIssuerName() { return issuerName; }
    public String getVerificationHash() { return verificationHash; }
    public String getPdfUrl() { return pdfUrl; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getCreatedAt() { return createdAt; }

    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }
}

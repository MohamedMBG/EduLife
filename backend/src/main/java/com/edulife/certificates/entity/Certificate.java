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

/**
 * JPA entity representing an issued certificate with point-in-time snapshots of learner, teacher, and course data.
 *
 * <p>Snapshots preserve the certificate as it was issued, even if profiles or course metadata change later.
 * Legacy columns from earlier schema versions are read as fallbacks via {@link #firstPresent}.</p>
 */
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

    @Column(name = "learner_name_snapshot", length = 200)
    private String learnerNameSnapshot;

    @Column(name = "teacher_name_snapshot", length = 200)
    private String teacherNameSnapshot;

    @Column(name = "course_title_snapshot", length = 255)
    private String courseTitleSnapshot;

    @Column(name = "course_level_snapshot", length = 50)
    private String courseLevelSnapshot;

    @Column(name = "student_name", insertable = false, updatable = false, length = 200)
    private String legacyStudentName;

    @Column(name = "course_title", insertable = false, updatable = false, length = 255)
    private String legacyCourseTitle;

    @Column(name = "issuer_name", insertable = false, updatable = false, length = 200)
    private String legacyIssuerName;

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
                       String learnerNameSnapshot, String teacherNameSnapshot,
                       String courseTitleSnapshot, String courseLevelSnapshot,
                       String verificationHash, String pdfUrl) {
        this.userId = userId;
        this.courseId = courseId;
        this.examAttemptId = examAttemptId;
        this.certificateNumber = certificateNumber;
        // Snapshots preserve the certificate as issued even if profiles or course metadata change later.
        this.learnerNameSnapshot = learnerNameSnapshot;
        this.teacherNameSnapshot = teacherNameSnapshot;
        this.courseTitleSnapshot = courseTitleSnapshot;
        this.courseLevelSnapshot = courseLevelSnapshot;
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
    public String getLearnerNameSnapshot() {
        return firstPresent(learnerNameSnapshot, legacyStudentName);
    }
    public String getTeacherNameSnapshot() {
        return firstPresent(teacherNameSnapshot, legacyIssuerName);
    }
    public String getCourseTitleSnapshot() {
        return firstPresent(courseTitleSnapshot, legacyCourseTitle);
    }
    public String getCourseLevelSnapshot() { return courseLevelSnapshot; }
    public String getVerificationHash() { return verificationHash; }
    public String getPdfUrl() { return pdfUrl; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getCreatedAt() { return createdAt; }

    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }

    private String firstPresent(String current, String legacy) {
        return current != null && !current.isBlank() ? current : legacy;
    }
}

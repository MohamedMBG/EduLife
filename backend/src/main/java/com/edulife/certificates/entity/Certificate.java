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

    @Column(name = "certificate_number", nullable = false, unique = true, length = 50)
    private String certificateNumber;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    protected Certificate() {}

    public Certificate(UUID userId, UUID courseId, String certificateNumber) {
        this.userId = userId;
        this.courseId = courseId;
        this.certificateNumber = certificateNumber;
    }

    @PrePersist
    void onCreate() {
        issuedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getCourseId() { return courseId; }
    public String getCertificateNumber() { return certificateNumber; }
    public Instant getIssuedAt() { return issuedAt; }
}

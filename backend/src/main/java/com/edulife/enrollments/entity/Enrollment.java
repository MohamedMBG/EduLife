package com.edulife.enrollments.entity;

import com.edulife.enrollments.model.EnrollmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "enrollments")
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "course_id", nullable = false, updatable = false)
    private UUID courseId;

    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private Instant enrolledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EnrollmentStatus status;

    protected Enrollment() {}

    public Enrollment(UUID userId, UUID courseId) {
        this.userId = userId;
        this.courseId = courseId;
    }

    @PrePersist
    void onCreate() {
        enrolledAt = Instant.now();
        if (status == null) {
            status = EnrollmentStatus.ACTIVE;
        }
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getCourseId() { return courseId; }
    public Instant getEnrolledAt() { return enrolledAt; }
    public EnrollmentStatus getStatus() { return status; }

    public void cancel() {
        this.status = EnrollmentStatus.CANCELLED;
    }
}

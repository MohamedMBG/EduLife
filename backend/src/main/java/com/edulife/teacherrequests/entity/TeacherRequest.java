package com.edulife.teacherrequests.entity;

import com.edulife.teacherrequests.model.RequestStatus;
import com.edulife.users.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a learner's request to be promoted to the teacher role.
 */
@Entity
@Table(name = "teacher_requests")
public class TeacherRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RequestStatus status = RequestStatus.PENDING;

    @Column(name = "motivation", columnDefinition = "TEXT")
    private String motivation;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    protected TeacherRequest() {}

    public TeacherRequest(User user, String motivation) {
        this.user = user;
        this.motivation = motivation;
    }

    @PrePersist
    void onCreate() {
        if (requestedAt == null) requestedAt = Instant.now();
    }

    public void approve(User admin) {
        this.status = RequestStatus.APPROVED;
        this.reviewedBy = admin;
        this.reviewedAt = Instant.now();
    }

    public void reject(User admin, String note) {
        this.status = RequestStatus.REJECTED;
        this.reviewedBy = admin;
        this.adminNote = note;
        this.reviewedAt = Instant.now();
    }

    public UUID getId()              { return id; }
    public User getUser()            { return user; }
    public RequestStatus getStatus() { return status; }
    public String getMotivation()    { return motivation; }
    public String getAdminNote()     { return adminNote; }
    public User getReviewedBy()      { return reviewedBy; }
    public Instant getRequestedAt()  { return requestedAt; }
    public Instant getReviewedAt()   { return reviewedAt; }
}

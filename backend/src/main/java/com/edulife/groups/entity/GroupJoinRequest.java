package com.edulife.groups.entity;

import com.edulife.groups.model.GroupJoinRequestStatus;
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
@Table(name = "group_join_requests")
public class GroupJoinRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "group_id", nullable = false, updatable = false)
    private UUID groupId;

    @Column(name = "requester_user_id", nullable = false, updatable = false)
    private UUID requesterUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GroupJoinRequestStatus status = GroupJoinRequestStatus.PENDING;

    @Column(name = "motivation", columnDefinition = "TEXT")
    private String motivation;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @Column(name = "reviewed_by_user_id")
    private UUID reviewedByUserId;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    protected GroupJoinRequest() {}

    public GroupJoinRequest(UUID groupId, UUID requesterUserId, String motivation) {
        this.groupId = groupId;
        this.requesterUserId = requesterUserId;
        this.motivation = motivation;
    }

    @PrePersist
    void onCreate() {
        if (requestedAt == null) {
            requestedAt = Instant.now();
        }
    }

    public void approve(UUID reviewerUserId) {
        this.status = GroupJoinRequestStatus.APPROVED;
        this.reviewedByUserId = reviewerUserId;
        this.reviewedAt = Instant.now();
    }

    public void reject(UUID reviewerUserId, String adminNote) {
        this.status = GroupJoinRequestStatus.REJECTED;
        this.reviewedByUserId = reviewerUserId;
        this.adminNote = adminNote;
        this.reviewedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getGroupId() { return groupId; }
    public UUID getRequesterUserId() { return requesterUserId; }
    public GroupJoinRequestStatus getStatus() { return status; }
    public String getMotivation() { return motivation; }
    public String getAdminNote() { return adminNote; }
    public UUID getReviewedByUserId() { return reviewedByUserId; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getReviewedAt() { return reviewedAt; }
}

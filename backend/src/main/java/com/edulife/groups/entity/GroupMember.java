package com.edulife.groups.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "group_members")
@IdClass(GroupMemberId.class)
public class GroupMember {

    @Id
    @Column(name = "group_id", nullable = false, updatable = false)
    private UUID groupId;

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt;

    protected GroupMember() {}

    public GroupMember(UUID groupId, UUID userId) {
        this.groupId = groupId;
        this.userId = userId;
    }

    @PrePersist
    void onCreate() {
        if (addedAt == null) {
            addedAt = Instant.now();
        }
    }

    public UUID getGroupId() { return groupId; }
    public UUID getUserId() { return userId; }
    public Instant getAddedAt() { return addedAt; }
}

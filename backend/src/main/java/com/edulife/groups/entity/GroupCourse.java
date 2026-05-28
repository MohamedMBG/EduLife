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
@Table(name = "group_courses")
@IdClass(GroupCourseId.class)
public class GroupCourse {

    @Id
    @Column(name = "group_id", nullable = false, updatable = false)
    private UUID groupId;

    @Id
    @Column(name = "course_id", nullable = false, updatable = false)
    private UUID courseId;

    @Column(name = "attached_at", nullable = false, updatable = false)
    private Instant attachedAt;

    protected GroupCourse() {}

    public GroupCourse(UUID groupId, UUID courseId) {
        this.groupId = groupId;
        this.courseId = courseId;
    }

    @PrePersist
    void onCreate() {
        if (attachedAt == null) {
            attachedAt = Instant.now();
        }
    }

    public UUID getGroupId() { return groupId; }
    public UUID getCourseId() { return courseId; }
    public Instant getAttachedAt() { return attachedAt; }
}

package com.edulife.courses.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lessons")
public class Lesson {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    // Keeping the parent section as a UUID avoids introducing a larger entity graph
    // before lesson detail and section traversal use cases are implemented.
    @Column(name = "course_section_id", nullable = false)
    private UUID courseSectionId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "lesson_type", nullable = false, length = 20)
    private String lessonType;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_preview", nullable = false)
    private boolean preview;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Lesson() {
    }

    // The migration defines defaults, but entity lifecycle hooks keep timestamps aligned
    // when lessons are created or updated through JPA instead of raw SQL.
    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    // Centralizing the audit timestamp here keeps service code focused on business logic.
    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getCourseSectionId() { return courseSectionId; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public String getLessonType() { return lessonType; }
    public Integer getEstimatedDurationMinutes() { return estimatedDurationMinutes; }
    public Integer getDisplayOrder() { return displayOrder; }
    public boolean isPreview() { return preview; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

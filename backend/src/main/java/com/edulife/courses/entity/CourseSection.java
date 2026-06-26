package com.edulife.courses.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapped to the {@code course_sections} table.
 *
 * <p>Groups lessons within a course and controls their display ordering.
 */
@Entity
@Table(name = "course_sections")
public class CourseSection {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    // Storing the parent course as a UUID keeps the section mapping simple for Sprint 2
    // and avoids introducing a bidirectional entity graph before detail endpoints need it.
    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CourseSection() {
    }

    /** Factory constructor for CMS section creation. displayOrder must be > 0 per DB constraint. */
    public CourseSection(UUID id, UUID courseId, String title, String description, int displayOrder) {
        this.id = id;
        this.courseId = courseId;
        this.title = title;
        this.description = description;
        this.displayOrder = displayOrder;
    }

    /** CMS update — all mutable fields can change together, including reorder. */
    public void update(String title, String description, Integer displayOrder) {
        this.title = title;
        this.description = description;
        if (displayOrder != null) {
            this.displayOrder = displayOrder;
        }
    }

    // The migration provides database defaults, but JPA lifecycle hooks keep timestamps
    // consistent when sections are created or updated through the application layer.
    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    // Updating the timestamp here keeps audit data out of controllers and services.
    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getCourseId() { return courseId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Integer getDisplayOrder() { return displayOrder; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

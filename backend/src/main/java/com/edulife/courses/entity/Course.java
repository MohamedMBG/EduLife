package com.edulife.courses.entity;

import com.edulife.courses.model.CourseStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "slug", nullable = false, unique = true, length = 160)
    private String slug;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "language_code", nullable = false, length = 10)
    private String languageCode;

    @Column(name = "level", length = 50)
    private String level;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CourseStatus status;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "published_at")
    private Instant publishedAt;

    // Keeping the author reference as a UUID avoids an early cross-module entity graph
    // while still preserving the teacher/admin ownership link from the schema.
    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Course() {
    }

    // JPA updates should keep the audit timestamp aligned even when the database default
    // is only applied during the first insert.
    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    // This keeps the row audit data trustworthy without pushing timestamp handling into services.
    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getSlug() { return slug; }
    public String getTitle() { return title; }
    public String getShortDescription() { return shortDescription; }
    public String getDescription() { return description; }
    public String getLanguageCode() { return languageCode; }
    public String getLevel() { return level; }
    public CourseStatus getStatus() { return status; }
    public String getImageUrl() { return imageUrl; }
    public Instant getPublishedAt() { return publishedAt; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

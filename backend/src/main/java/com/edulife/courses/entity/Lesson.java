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

    @Column(name = "content_url", length = 2048)
    private String contentUrl;

    @Column(name = "content_body", columnDefinition = "TEXT")
    private String contentBody;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Lesson() {
    }

    /** Factory constructor for CMS lesson creation. lessonType must be VIDEO, ARTICLE, or RESOURCE. */
    public Lesson(UUID id, UUID courseSectionId, String title, String summary,
                  String lessonType, Integer estimatedDurationMinutes, int displayOrder,
                  boolean preview, String contentUrl, String contentBody) {
        this.id = id;
        this.courseSectionId = courseSectionId;
        this.title = title;
        this.summary = summary;
        this.lessonType = lessonType;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.displayOrder = displayOrder;
        this.preview = preview;
        this.contentUrl = contentUrl;
        this.contentBody = contentBody;
    }

    /** CMS update — all mutable lesson fields can be changed. */
    public void update(String title, String summary, String lessonType,
                       Integer estimatedDurationMinutes, Integer displayOrder,
                       Boolean preview, String contentUrl, String contentBody) {
        if (title != null) this.title = title;
        if (summary != null) this.summary = summary;
        if (lessonType != null) this.lessonType = lessonType;
        if (estimatedDurationMinutes != null) this.estimatedDurationMinutes = estimatedDurationMinutes;
        if (displayOrder != null) this.displayOrder = displayOrder;
        if (preview != null) this.preview = preview;
        if (contentUrl != null) this.contentUrl = contentUrl;
        if (contentBody != null) this.contentBody = contentBody;
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
    public String getContentUrl() { return contentUrl; }
    public String getContentBody() { return contentBody; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

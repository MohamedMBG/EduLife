package com.edulife.progress.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "course_progress")
public class CourseProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "completed_lessons", nullable = false)
    private int completedLessons;

    @Column(name = "total_lessons", nullable = false)
    private int totalLessons;

    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

    protected CourseProgress() {}

    public CourseProgress(UUID userId, UUID courseId, int completedLessons, int totalLessons) {
        this.userId           = userId;
        this.courseId         = courseId;
        this.completedLessons = completedLessons;
        this.totalLessons     = totalLessons;
        this.lastUpdatedAt    = Instant.now();
    }

    public void update(int completedLessons, int totalLessons) {
        this.completedLessons = completedLessons;
        this.totalLessons     = totalLessons;
        this.lastUpdatedAt    = Instant.now();
    }

    public UUID getId()               { return id; }
    public UUID getUserId()           { return userId; }
    public UUID getCourseId()         { return courseId; }
    public int getCompletedLessons()  { return completedLessons; }
    public int getTotalLessons()      { return totalLessons; }
    public Instant getLastUpdatedAt() { return lastUpdatedAt; }
}

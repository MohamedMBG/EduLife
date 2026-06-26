package com.edulife.progress.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity recording that a learner has completed a specific lesson.
 *
 * <p>Each row represents a single lesson completion event with a timestamp.</p>
 */
@Entity
@Table(name = "lesson_progress")
public class LessonProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "lesson_id", nullable = false)
    private UUID lessonId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    protected LessonProgress() {}

    public LessonProgress(UUID userId, UUID lessonId, UUID courseId) {
        this.userId      = userId;
        this.lessonId    = lessonId;
        this.courseId    = courseId;
        this.completedAt = Instant.now();
    }

    public UUID getId()          { return id; }
    public UUID getUserId()      { return userId; }
    public UUID getLessonId()    { return lessonId; }
    public UUID getCourseId()    { return courseId; }
    public Instant getCompletedAt() { return completedAt; }
}

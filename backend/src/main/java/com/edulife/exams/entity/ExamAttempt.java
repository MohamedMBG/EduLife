package com.edulife.exams.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "exam_attempts")
public class ExamAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "exam_id", nullable = false, updatable = false)
    private UUID examId;

    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "passed", nullable = false)
    private boolean passed;

    @Column(name = "taken_at", nullable = false, updatable = false)
    private Instant takenAt;

    protected ExamAttempt() {}

    public ExamAttempt(UUID userId, UUID examId, int score, boolean passed) {
        this.userId = userId;
        this.examId = examId;
        this.score  = score;
        this.passed = passed;
    }

    @PrePersist
    void onCreate() {
        takenAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getExamId() { return examId; }
    public int getScore() { return score; }
    public boolean isPassed() { return passed; }
    public Instant getTakenAt() { return takenAt; }
}

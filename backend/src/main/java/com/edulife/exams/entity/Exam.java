package com.edulife.exams.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "exams")
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "course_id", nullable = false, updatable = false, unique = true)
    private UUID courseId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "pass_score", nullable = false)
    private int passScore;

    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes;

    protected Exam() {}

    /** Constructor for CMS exam authoring. passScore is a percentage (0–100). */
    public Exam(UUID courseId, String title, int passScore, Integer timeLimitMinutes) {
        this.courseId = courseId;
        this.title = title;
        this.passScore = passScore;
        this.timeLimitMinutes = timeLimitMinutes;
    }

    public UUID getId() { return id; }
    public UUID getCourseId() { return courseId; }
    public String getTitle() { return title; }
    public int getPassScore() { return passScore; }
    public Integer getTimeLimitMinutes() { return timeLimitMinutes; }
}

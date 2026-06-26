package com.edulife.exams.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** JPA entity representing an answer choice for an exam question; the {@code correct} flag is never sent to clients. */
@Entity
@Table(name = "exam_choices")
public class ExamChoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "question_id", nullable = false, updatable = false)
    private UUID questionId;

    @Column(name = "choice_text", nullable = false, columnDefinition = "TEXT")
    private String choiceText;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    protected ExamChoice() {}

    /** Constructor for CMS exam authoring. Exactly one choice per question should have correct=true. */
    public ExamChoice(UUID questionId, String choiceText, boolean correct) {
        this.questionId = questionId;
        this.choiceText = choiceText;
        this.correct = correct;
    }

    public UUID getId() { return id; }
    public UUID getQuestionId() { return questionId; }
    public String getChoiceText() { return choiceText; }
    public boolean isCorrect() { return correct; }
}

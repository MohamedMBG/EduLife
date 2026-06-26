package com.edulife.exams.exception;

import java.time.Instant;

/** Thrown when a learner attempts an exam during the 72-hour cooldown after 2 consecutive failures. */
public class ExamCooldownException extends RuntimeException {

    private final Instant cooldownEndsAt;

    public ExamCooldownException(Instant cooldownEndsAt) {
        super("Too many failed attempts. You can retake the exam after the cooldown period expires");
        this.cooldownEndsAt = cooldownEndsAt;
    }

    public Instant getCooldownEndsAt() {
        return cooldownEndsAt;
    }
}

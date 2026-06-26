package com.baghdad.edulife.features.courses.model;

/** Backend response DTO describing a learner's current exam eligibility, including pass state, attempt count, and cooldown status. */
public class ExamStatusResponse {
    public String examId;
    public boolean passed;
    public int failedAttempts;
    public int maxAttemptsBeforeCooldown;
    public boolean inCooldown;
    public String cooldownEndsAt;
}

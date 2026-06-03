package com.baghdad.edulife.features.courses.model;

public class ExamStatusResponse {
    public String examId;
    public boolean passed;
    public int failedAttempts;
    public int maxAttemptsBeforeCooldown;
    public boolean inCooldown;
    public String cooldownEndsAt;
}

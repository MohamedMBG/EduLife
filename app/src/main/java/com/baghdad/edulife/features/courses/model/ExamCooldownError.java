package com.baghdad.edulife.features.courses.model;

/** Error response body returned by the backend when an exam attempt is blocked by the 72-hour cooldown period. */
public class ExamCooldownError {
    public int status;
    public String message;
    public String cooldownEndsAt;
}

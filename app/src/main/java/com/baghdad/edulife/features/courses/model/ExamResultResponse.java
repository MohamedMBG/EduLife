package com.baghdad.edulife.features.courses.model;

/** Backend response DTO returned after submitting an exam, containing the score, pass/fail status, and optional certificate number. */
public class ExamResultResponse {
    public String examId;
    public int score;
    public int passScore;
    public boolean passed;
    public String certificateNumber;
    public int attemptsUsed;
    public String cooldownEndsAt;
}

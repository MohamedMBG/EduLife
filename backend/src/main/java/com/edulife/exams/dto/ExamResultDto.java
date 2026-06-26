package com.edulife.exams.dto;

import java.time.Instant;
import java.util.UUID;

/** Response DTO returned after exam submission with score, pass/fail result, and optional certificate number. */
public record ExamResultDto(
        UUID examId,
        int score,
        int passScore,
        boolean passed,
        String certificateNumber,
        int attemptsUsed,
        Instant cooldownEndsAt
) {}

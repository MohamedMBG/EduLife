package com.edulife.exams.dto;

import java.time.Instant;
import java.util.UUID;

public record ExamResultDto(
        UUID examId,
        int score,
        int passScore,
        boolean passed,
        String certificateNumber,
        int attemptsUsed,
        Instant cooldownEndsAt
) {}

package com.edulife.exams.dto;

import java.time.Instant;
import java.util.UUID;

public record ExamStatusDto(
        UUID examId,
        boolean passed,
        int failedAttempts,
        int maxAttemptsBeforeCooldown,
        boolean inCooldown,
        Instant cooldownEndsAt
) {}

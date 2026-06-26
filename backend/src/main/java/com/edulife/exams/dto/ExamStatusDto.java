package com.edulife.exams.dto;

import java.time.Instant;
import java.util.UUID;

/** Response DTO representing a learner's exam attempt status, including cooldown state after 2 failures. */
public record ExamStatusDto(
        UUID examId,
        boolean passed,
        int failedAttempts,
        int maxAttemptsBeforeCooldown,
        boolean inCooldown,
        Instant cooldownEndsAt
) {}

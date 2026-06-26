package com.edulife.gamification.dto;

import java.time.Instant;

/**
 * API response DTO for a badge, combining its catalog definition with the learner's unlock status.
 */
public record BadgeDto(
        String id,
        String label,
        String rarity,
        String unlockDescription,
        boolean unlocked,
        Instant unlockedAt
) {
}

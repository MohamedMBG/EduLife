package com.edulife.gamification.dto;

import java.time.Instant;

public record BadgeDto(
        String id,
        String label,
        String rarity,
        String unlockDescription,
        boolean unlocked,
        Instant unlockedAt
) {
}

package com.edulife.gamification.dto;

import java.util.UUID;

public record LeaderboardEntryDto(
        int rank,
        UUID userId,
        String displayName,
        int totalXp,
        int level,
        String levelName
) {
}

package com.edulife.gamification.dto;

import java.util.UUID;

/**
 * API response DTO for a single entry in the global XP leaderboard.
 */
public record LeaderboardEntryDto(
        int rank,
        UUID userId,
        String displayName,
        int totalXp,
        int level,
        String levelName
) {
}

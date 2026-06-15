package com.edulife.gamification.dto;

import java.time.LocalDate;
import java.util.List;

public record GamificationStateDto(
        int totalXp,
        int level,
        String levelName,
        int currentLevelXp,
        int nextLevelXp,
        int xpIntoLevel,
        int xpForNextLevel,
        int currentStreak,
        int longestStreak,
        LocalDate lastActivityDate,
        List<BadgeDto> badges
) {
}

package com.edulife.gamification.model;

public record BadgeDefinition(
        String id,
        String label,
        BadgeRarity rarity,
        String unlockDescription
) {
}

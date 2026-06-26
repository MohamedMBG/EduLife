package com.edulife.gamification.model;

/**
 * Immutable definition of a badge: its id, display label, rarity tier, and unlock description.
 */
public record BadgeDefinition(
        String id,
        String label,
        BadgeRarity rarity,
        String unlockDescription
) {
}

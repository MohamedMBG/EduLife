package com.baghdad.edulife.features.gamification.model;

/**
 * Holds the learner's current level information for display in the gamification UI.
 * Level boundaries follow a nature-themed progression (Seedling → Luminary).
 */
public class LevelInfo {

    /** Current level number (1–10) */
    public final int level;

    /** Human-readable level title (e.g. "Seedling", "Scholar") */
    public final String title;

    /** Total XP the learner currently has */
    public final int currentXp;

    /** XP threshold where the current level starts */
    public final int xpForCurrentLevel;

    /** XP threshold where the next level starts (Integer.MAX_VALUE for max level) */
    public final int xpForNextLevel;

    /**
     * Progress percentage within the current level (0–100).
     * Used to fill the XP progress bar and level ring.
     */
    public final int progressPercent;

    public LevelInfo(int level, String title, int currentXp,
                     int xpForCurrentLevel, int xpForNextLevel, int progressPercent) {
        this.level = level;
        this.title = title;
        this.currentXp = currentXp;
        this.xpForCurrentLevel = xpForCurrentLevel;
        this.xpForNextLevel = xpForNextLevel;
        this.progressPercent = progressPercent;
    }
}

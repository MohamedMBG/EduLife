package com.baghdad.edulife.features.analytics.model;

/**
 * Streak / consistency card. The single source of truth for streaks is the gamification module;
 * {@link #currentStreak} and {@link #bestStreak} are populated from GET /gamification/me, while
 * {@link #daysStudiedThisWeek} is derived alongside the weekly chart from lesson timestamps.
 */
public class LearningStreakSummary {
    public final int currentStreak;
    public final int bestStreak;
    public final int daysStudiedThisWeek;

    public LearningStreakSummary(int currentStreak, int bestStreak, int daysStudiedThisWeek) {
        this.currentStreak = currentStreak;
        this.bestStreak = bestStreak;
        this.daysStudiedThisWeek = daysStudiedThisWeek;
    }
}

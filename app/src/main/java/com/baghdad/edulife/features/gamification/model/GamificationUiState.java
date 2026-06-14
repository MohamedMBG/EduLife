package com.baghdad.edulife.features.gamification.model;

import java.util.List;

/**
 * UI state for the gamification dashboard screen.
 * Aggregates all gamification data into a single observable snapshot
 * so the fragment can render the entire screen from one LiveData emission.
 */
public class GamificationUiState {

    /** Total XP the learner has earned across all time */
    public final int totalXp;

    /** Current level info with title and progress */
    public final LevelInfo levelInfo;

    /** Current consecutive-day learning streak count */
    public final int streak;

    /** All defined badges with earned/locked status */
    public final List<Badge> badges;

    /** Count of lessons the learner has completed (drives badge checks) */
    public final int lessonsCompleted;

    /** Count of courses the learner has enrolled in */
    public final int coursesEnrolled;

    /** Count of certificates earned */
    public final int certificatesEarned;

    public GamificationUiState(int totalXp, LevelInfo levelInfo, int streak,
                               List<Badge> badges, int lessonsCompleted,
                               int coursesEnrolled, int certificatesEarned) {
        this.totalXp = totalXp;
        this.levelInfo = levelInfo;
        this.streak = streak;
        this.badges = badges;
        this.lessonsCompleted = lessonsCompleted;
        this.coursesEnrolled = coursesEnrolled;
        this.certificatesEarned = certificatesEarned;
    }
}

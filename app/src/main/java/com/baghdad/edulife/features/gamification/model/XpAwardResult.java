package com.baghdad.edulife.features.gamification.model;

import java.util.List;

/**
 * Result returned by XpEngine after processing an XP-worthy event.
 * Contains all information needed to update the UI: how much XP was earned,
 * whether the learner leveled up, and any newly unlocked badges.
 */
public class XpAwardResult {

    /** Amount of XP earned from this specific event */
    public final int xpAwarded;

    /** Learner's new total XP after the award */
    public final int newTotalXp;

    /** Badges unlocked by this event (empty list if none) */
    public final List<Badge> newBadges;

    /** True if the learner crossed a level boundary with this award */
    public final boolean didLevelUp;

    /** Updated level information reflecting the post-award state */
    public final LevelInfo newLevelInfo;

    public XpAwardResult(int xpAwarded, int newTotalXp, List<Badge> newBadges,
                         boolean didLevelUp, LevelInfo newLevelInfo) {
        this.xpAwarded = xpAwarded;
        this.newTotalXp = newTotalXp;
        this.newBadges = newBadges;
        this.didLevelUp = didLevelUp;
        this.newLevelInfo = newLevelInfo;
    }
}

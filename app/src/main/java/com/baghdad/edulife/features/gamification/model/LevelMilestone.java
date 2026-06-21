package com.baghdad.edulife.features.gamification.model;

public class LevelMilestone {

    public enum State { DONE, CURRENT, LOCKED }

    public final int level;
    public final String title;
    public final int xpThreshold;
    public final State state;

    public LevelMilestone(int level, String title, int xpThreshold, State state) {
        this.level = level;
        this.title = title;
        this.xpThreshold = xpThreshold;
        this.state = state;
    }
}

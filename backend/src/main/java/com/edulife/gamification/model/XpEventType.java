package com.edulife.gamification.model;

/**
 * Enumeration of all XP-awarding events with their fixed point values.
 * These values are the single source of truth shared across Android and Web.
 */
public enum XpEventType {
    LESSON_COMPLETED(25),
    COURSE_COMPLETED(100),
    EXAM_PASSED(150),
    CERTIFICATE_EARNED(200),
    ENROLLMENT(10),
    DAILY_LOGIN(5),
    STREAK_BONUS_3(30),
    STREAK_BONUS_7(75);

    private final int xp;

    XpEventType(int xp) {
        this.xp = xp;
    }

    public int xp() {
        return xp;
    }
}

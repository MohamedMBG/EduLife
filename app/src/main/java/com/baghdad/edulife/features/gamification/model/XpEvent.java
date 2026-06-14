package com.baghdad.edulife.features.gamification.model;

/**
 * Enum of all actions that can earn XP in the gamification system.
 * Each event maps to a fixed XP reward defined in XpEngine.
 *
 * XP-worthy events are limited to meaningful learning milestones so the
 * system cannot be gamed by trivial interactions.
 */
public enum XpEvent {
    /** Learner marks a lesson as complete */
    LESSON_COMPLETE,

    /** All lessons in a course are marked complete */
    COURSE_COMPLETE,

    /** Learner passes the final MCQ exam for a course */
    EXAM_PASS,

    /** A certificate is generated after passing the exam */
    CERTIFICATE_EARNED,

    /** Learner enrolls in a new course */
    ENROLLMENT,

    /** First learning action of the calendar day */
    DAILY_LOGIN,

    /** Bonus awarded when a 3-day streak is reached */
    STREAK_3_BONUS,

    /** Bonus awarded when a 7-day streak is reached */
    STREAK_7_BONUS
}

package com.baghdad.edulife.features.gamification.data;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.gamification.model.Badge;
import com.baghdad.edulife.features.gamification.model.BadgeRarity;
import com.baghdad.edulife.features.gamification.model.LevelInfo;
import com.baghdad.edulife.features.gamification.model.XpAwardResult;
import com.baghdad.edulife.features.gamification.model.XpEvent;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure business logic engine for the gamification system.
 *
 * Shared spec: see root CLAUDE.md → ## Gamification (Shared Spec).
 * XP values, level thresholds, level names, and badge defs MUST match the web client.
 */
public class XpEngine {

    // ── XP reward table (shared spec) ───────────────────────────────────

    public static final int XP_LESSON_COMPLETE   = 25;
    public static final int XP_COURSE_COMPLETE   = 100;
    public static final int XP_EXAM_PASS         = 150;
    public static final int XP_CERTIFICATE       = 200;
    public static final int XP_ENROLLMENT        = 10;
    public static final int XP_DAILY_LOGIN       = 5;
    public static final int XP_STREAK_3_BONUS    = 30;
    public static final int XP_STREAK_7_BONUS    = 75;

    // ── Level thresholds (shared spec) ──────────────────────────────────

    public static final int[] LEVEL_THRESHOLDS = {
            0, 250, 600, 1100, 1800, 2700, 3900, 5500, 7500, 10000
    };

    public static final String[] LEVEL_TITLES = {
            "Novice", "Curious", "Explorer", "Seeker", "Thinker",
            "Achiever", "Scholar", "Expert", "Sage", "Master"
    };

    // ── Badge IDs (shared spec) ─────────────────────────────────────────

    public static final String BADGE_FIRST_FLAME    = "first_flame";
    public static final String BADGE_BOOKWORM       = "bookworm";
    public static final String BADGE_SPEED_RUN      = "speed_run";
    public static final String BADGE_SHARP_MIND     = "sharp_mind";
    public static final String BADGE_GRADUATE       = "graduate";
    public static final String BADGE_ON_A_ROLL      = "on_a_roll";
    public static final String BADGE_DEDICATED      = "dedicated";
    public static final String BADGE_STAR_LEARNER   = "star_learner";
    public static final String BADGE_SCHOLAR        = "scholar";
    public static final String BADGE_MASTER         = "master";
    public static final String BADGE_TROPHY_HUNTER  = "trophy_hunter";
    public static final String BADGE_INFERNO        = "inferno";

    private final GamificationPreferences prefs;

    public XpEngine(GamificationPreferences prefs) {
        this.prefs = prefs;
    }

    public XpAwardResult awardXp(XpEvent event) {
        int xpBefore = prefs.getTotalXp();
        int levelBefore = computeLevel(xpBefore);
        int xpEarned = 0;

        switch (event) {
            case LESSON_COMPLETE:
                prefs.incrementLessonsCompleted();
                xpEarned = XP_LESSON_COMPLETE;
                break;
            case COURSE_COMPLETE:
                prefs.incrementCoursesCompleted();
                xpEarned = XP_COURSE_COMPLETE;
                break;
            case EXAM_PASS:
                prefs.incrementExamsPassed();
                xpEarned = XP_EXAM_PASS;
                break;
            case CERTIFICATE_EARNED:
                prefs.incrementCertificatesEarned();
                xpEarned = XP_CERTIFICATE;
                break;
            case ENROLLMENT:
                prefs.incrementCoursesEnrolled();
                xpEarned = XP_ENROLLMENT;
                break;
            case DAILY_LOGIN:
                xpEarned = XP_DAILY_LOGIN;
                break;
            case STREAK_3_BONUS:
                xpEarned = XP_STREAK_3_BONUS;
                break;
            case STREAK_7_BONUS:
                xpEarned = XP_STREAK_7_BONUS;
                break;
        }

        prefs.addXp(xpEarned);

        if (isLearningAction(event)) {
            xpEarned += updateStreak();
        }
        int newTotal = prefs.getTotalXp();

        List<Badge> newBadges = evaluateBadges(computeLevel(newTotal));
        int levelAfter = computeLevel(newTotal);
        boolean didLevelUp = levelAfter > levelBefore;
        LevelInfo newLevelInfo = computeLevelInfo(newTotal);

        return new XpAwardResult(xpEarned, newTotal, newBadges, didLevelUp, newLevelInfo);
    }

    private int updateStreak() {
        int bonusXp = 0;
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String lastActive = prefs.getLastActiveDate();

        if (today.equals(lastActive)) {
            return 0;
        }

        String yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);

        if (yesterday.equals(lastActive)) {
            int newStreak = prefs.getStreak() + 1;
            prefs.setStreak(newStreak);

            if (newStreak >= 3 && !prefs.isStreak3Awarded()) {
                bonusXp += XP_STREAK_3_BONUS;
                prefs.addXp(XP_STREAK_3_BONUS);
                prefs.setStreak3Awarded(true);
            }
            if (newStreak >= 7 && !prefs.isStreak7Awarded()) {
                bonusXp += XP_STREAK_7_BONUS;
                prefs.addXp(XP_STREAK_7_BONUS);
                prefs.setStreak7Awarded(true);
            }
        } else if (lastActive.isEmpty()) {
            prefs.setStreak(1);
        } else {
            prefs.setStreak(1);
            prefs.resetStreakBonuses();
        }

        prefs.setLastActiveDate(today);
        return bonusXp;
    }

    private boolean isLearningAction(XpEvent event) {
        return event == XpEvent.LESSON_COMPLETE
                || event == XpEvent.COURSE_COMPLETE
                || event == XpEvent.EXAM_PASS
                || event == XpEvent.CERTIFICATE_EARNED
                || event == XpEvent.ENROLLMENT;
    }

    /**
     * Idempotent reconciliation. Wipes legacy badge ids from before the shared-spec
     * migration and retroactively unlocks any spec badges already earned by existing
     * counters. Safe to call on every launch.
     */
    public List<Badge> reconcileBadges() {
        prefs.purgeLegacyBadges();
        return evaluateBadges(computeLevel(prefs.getTotalXp()));
    }

    // ── Badge evaluation (shared spec) ──────────────────────────────────

    private List<Badge> evaluateBadges(int currentLevel) {
        List<Badge> newlyEarned = new ArrayList<>();

        if (!prefs.hasBadge(BADGE_FIRST_FLAME) && prefs.getLessonsCompleted() >= 1) {
            prefs.addBadge(BADGE_FIRST_FLAME);
            newlyEarned.add(makeBadge(BADGE_FIRST_FLAME, true));
        }
        if (!prefs.hasBadge(BADGE_BOOKWORM) && prefs.getLessonsCompleted() >= 10) {
            prefs.addBadge(BADGE_BOOKWORM);
            newlyEarned.add(makeBadge(BADGE_BOOKWORM, true));
        }
        if (!prefs.hasBadge(BADGE_SPEED_RUN) && prefs.getLessonsToday() >= 3) {
            prefs.addBadge(BADGE_SPEED_RUN);
            newlyEarned.add(makeBadge(BADGE_SPEED_RUN, true));
        }
        if (!prefs.hasBadge(BADGE_SHARP_MIND) && prefs.getExamsPassed() >= 1) {
            prefs.addBadge(BADGE_SHARP_MIND);
            newlyEarned.add(makeBadge(BADGE_SHARP_MIND, true));
        }
        if (!prefs.hasBadge(BADGE_GRADUATE) && prefs.getCertificatesEarned() >= 1) {
            prefs.addBadge(BADGE_GRADUATE);
            newlyEarned.add(makeBadge(BADGE_GRADUATE, true));
        }
        if (!prefs.hasBadge(BADGE_ON_A_ROLL) && prefs.getLessonsLast7Days() >= 5) {
            prefs.addBadge(BADGE_ON_A_ROLL);
            newlyEarned.add(makeBadge(BADGE_ON_A_ROLL, true));
        }
        if (!prefs.hasBadge(BADGE_DEDICATED) && prefs.getLongestStreak() >= 14) {
            prefs.addBadge(BADGE_DEDICATED);
            newlyEarned.add(makeBadge(BADGE_DEDICATED, true));
        }
        if (!prefs.hasBadge(BADGE_STAR_LEARNER) && prefs.getLongestStreak() >= 30) {
            prefs.addBadge(BADGE_STAR_LEARNER);
            newlyEarned.add(makeBadge(BADGE_STAR_LEARNER, true));
        }
        if (!prefs.hasBadge(BADGE_SCHOLAR) && currentLevel >= 7) {
            prefs.addBadge(BADGE_SCHOLAR);
            newlyEarned.add(makeBadge(BADGE_SCHOLAR, true));
        }
        if (!prefs.hasBadge(BADGE_MASTER) && currentLevel >= 10) {
            prefs.addBadge(BADGE_MASTER);
            newlyEarned.add(makeBadge(BADGE_MASTER, true));
        }
        if (!prefs.hasBadge(BADGE_TROPHY_HUNTER) && prefs.getCertificatesEarned() >= 3) {
            prefs.addBadge(BADGE_TROPHY_HUNTER);
            newlyEarned.add(makeBadge(BADGE_TROPHY_HUNTER, true));
        }
        if (!prefs.hasBadge(BADGE_INFERNO) && prefs.getLongestStreak() >= 60) {
            prefs.addBadge(BADGE_INFERNO);
            newlyEarned.add(makeBadge(BADGE_INFERNO, true));
        }

        return newlyEarned;
    }

    // ── Level computation ───────────────────────────────────────────────

    public int computeLevel(int totalXp) {
        int level = 1;
        for (int i = LEVEL_THRESHOLDS.length - 1; i >= 0; i--) {
            if (totalXp >= LEVEL_THRESHOLDS[i]) {
                level = i + 1;
                break;
            }
        }
        return level;
    }

    public LevelInfo computeLevelInfo(int totalXp) {
        int level = computeLevel(totalXp);
        int idx = level - 1;
        String title = LEVEL_TITLES[idx];
        int currentThreshold = LEVEL_THRESHOLDS[idx];
        int nextThreshold = (idx + 1 < LEVEL_THRESHOLDS.length)
                ? LEVEL_THRESHOLDS[idx + 1]
                : Integer.MAX_VALUE;

        int progress;
        if (nextThreshold == Integer.MAX_VALUE) {
            progress = 100;
        } else {
            int range = nextThreshold - currentThreshold;
            int within = totalXp - currentThreshold;
            progress = (range > 0) ? (within * 100 / range) : 100;
        }

        return new LevelInfo(level, title, totalXp, currentThreshold, nextThreshold, progress);
    }

    // ── Badge factory ───────────────────────────────────────────────────

    public List<Badge> getAllBadges() {
        List<Badge> badges = new ArrayList<>();
        badges.add(makeBadge(BADGE_FIRST_FLAME, prefs.hasBadge(BADGE_FIRST_FLAME)));
        badges.add(makeBadge(BADGE_BOOKWORM, prefs.hasBadge(BADGE_BOOKWORM)));
        badges.add(makeBadge(BADGE_SPEED_RUN, prefs.hasBadge(BADGE_SPEED_RUN)));
        badges.add(makeBadge(BADGE_SHARP_MIND, prefs.hasBadge(BADGE_SHARP_MIND)));
        badges.add(makeBadge(BADGE_GRADUATE, prefs.hasBadge(BADGE_GRADUATE)));
        badges.add(makeBadge(BADGE_ON_A_ROLL, prefs.hasBadge(BADGE_ON_A_ROLL)));
        badges.add(makeBadge(BADGE_DEDICATED, prefs.hasBadge(BADGE_DEDICATED)));
        badges.add(makeBadge(BADGE_STAR_LEARNER, prefs.hasBadge(BADGE_STAR_LEARNER)));
        badges.add(makeBadge(BADGE_SCHOLAR, prefs.hasBadge(BADGE_SCHOLAR)));
        badges.add(makeBadge(BADGE_MASTER, prefs.hasBadge(BADGE_MASTER)));
        badges.add(makeBadge(BADGE_TROPHY_HUNTER, prefs.hasBadge(BADGE_TROPHY_HUNTER)));
        badges.add(makeBadge(BADGE_INFERNO, prefs.hasBadge(BADGE_INFERNO)));
        return badges;
    }

    private Badge makeBadge(String id, boolean earned) {
        switch (id) {
            case BADGE_FIRST_FLAME:
                return new Badge(id, "First Flame", "Complete your first lesson",
                        R.drawable.ic_badge_first_steps, "🔥", BadgeRarity.COMMON, earned);
            case BADGE_BOOKWORM:
                return new Badge(id, "Bookworm", "Complete 10 lessons",
                        R.drawable.ic_badge_bookworm, "📚", BadgeRarity.RARE, earned);
            case BADGE_SPEED_RUN:
                return new Badge(id, "Speed Run", "3 lessons in one day",
                        R.drawable.ic_badge_on_fire, "⚡", BadgeRarity.RARE, earned);
            case BADGE_SHARP_MIND:
                return new Badge(id, "Sharp Mind", "Pass any exam",
                        R.drawable.ic_badge_champion, "🎯", BadgeRarity.EPIC, earned);
            case BADGE_GRADUATE:
                return new Badge(id, "Graduate", "Earn your first certificate",
                        R.drawable.ic_badge_certified, "🎓", BadgeRarity.EPIC, earned);
            case BADGE_ON_A_ROLL:
                return new Badge(id, "On A Roll", "5 lessons in a week",
                        R.drawable.ic_badge_polymath, "📈", BadgeRarity.COMMON, earned);
            case BADGE_DEDICATED:
                return new Badge(id, "Dedicated", "14-day streak",
                        R.drawable.ic_badge_dedicated, "🛡️", BadgeRarity.EPIC, earned);
            case BADGE_STAR_LEARNER:
                return new Badge(id, "Star Learner", "30-day streak",
                        R.drawable.ic_badge_unstoppable, "⭐", BadgeRarity.LEGENDARY, earned);
            case BADGE_SCHOLAR:
                return new Badge(id, "Scholar", "Reach level 7",
                        R.drawable.ic_badge_bookworm, "📜", BadgeRarity.EPIC, earned);
            case BADGE_MASTER:
                return new Badge(id, "Master", "Reach level 10",
                        R.drawable.ic_badge_champion, "👑", BadgeRarity.LEGENDARY, earned);
            case BADGE_TROPHY_HUNTER:
                return new Badge(id, "Trophy Hunter", "Earn 3 certificates",
                        R.drawable.ic_badge_champion, "🏆", BadgeRarity.LEGENDARY, earned);
            case BADGE_INFERNO:
                return new Badge(id, "Inferno", "60-day streak",
                        R.drawable.ic_badge_on_fire, "🔥", BadgeRarity.LEGENDARY, earned);
            default:
                return new Badge(id, "Unknown", "",
                        R.drawable.ic_xp_star, "?", BadgeRarity.COMMON, false);
        }
    }
}

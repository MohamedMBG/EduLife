package com.baghdad.edulife.features.gamification.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Persists gamification state locally using SharedPreferences.
 *
 * Shared spec: see root CLAUDE.md → ## Gamification (Shared Spec).
 * Constants and rules here must match the web client.
 */
public class GamificationPreferences {

    private static final String PREFS_NAME = "edulife_gamification";

    private static final String KEY_TOTAL_XP = "total_xp";
    private static final String KEY_STREAK = "streak_count";
    private static final String KEY_LONGEST_STREAK = "longest_streak";
    private static final String KEY_LAST_ACTIVE_DATE = "last_active_date";
    private static final String KEY_EARNED_BADGES = "earned_badges";
    private static final String KEY_LESSONS_COMPLETED = "lessons_completed";
    private static final String KEY_COURSES_ENROLLED = "courses_enrolled";
    private static final String KEY_COURSES_COMPLETED = "courses_completed";
    private static final String KEY_EXAMS_PASSED = "exams_passed";
    private static final String KEY_CERTIFICATES_EARNED = "certificates_earned";
    private static final String KEY_STREAK_3_AWARDED = "streak_3_awarded";
    private static final String KEY_STREAK_7_AWARDED = "streak_7_awarded";

    // Speed-run / on-a-roll badge tracking
    private static final String KEY_LESSONS_TODAY_COUNT = "lessons_today_count";
    private static final String KEY_LESSONS_TODAY_DATE = "lessons_today_date";
    private static final String KEY_LESSON_DATES = "lesson_dates";

    // Cap stored lesson dates to bound prefs size; 90 days covers all rolling windows.
    private static final int LESSON_DATES_MAX = 90;

    private final SharedPreferences prefs;

    public GamificationPreferences(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ── XP ──────────────────────────────────────────────────────────────

    public int getTotalXp() {
        return prefs.getInt(KEY_TOTAL_XP, 0);
    }

    public void setTotalXp(int xp) {
        prefs.edit().putInt(KEY_TOTAL_XP, Math.max(0, xp)).apply();
    }

    public int addXp(int delta) {
        int newTotal = getTotalXp() + delta;
        setTotalXp(newTotal);
        return newTotal;
    }

    // ── Streak ──────────────────────────────────────────────────────────

    public int getStreak() {
        return prefs.getInt(KEY_STREAK, 0);
    }

    public void setStreak(int streak) {
        int clamped = Math.max(0, streak);
        prefs.edit().putInt(KEY_STREAK, clamped).apply();
        if (clamped > getLongestStreak()) {
            prefs.edit().putInt(KEY_LONGEST_STREAK, clamped).apply();
        }
    }

    public int getLongestStreak() {
        return prefs.getInt(KEY_LONGEST_STREAK, 0);
    }

    public String getLastActiveDate() {
        return prefs.getString(KEY_LAST_ACTIVE_DATE, "");
    }

    public void setLastActiveDate(String date) {
        prefs.edit().putString(KEY_LAST_ACTIVE_DATE, date).apply();
    }

    public boolean isStreak3Awarded() {
        return prefs.getBoolean(KEY_STREAK_3_AWARDED, false);
    }

    public void setStreak3Awarded(boolean awarded) {
        prefs.edit().putBoolean(KEY_STREAK_3_AWARDED, awarded).apply();
    }

    public boolean isStreak7Awarded() {
        return prefs.getBoolean(KEY_STREAK_7_AWARDED, false);
    }

    public void setStreak7Awarded(boolean awarded) {
        prefs.edit().putBoolean(KEY_STREAK_7_AWARDED, awarded).apply();
    }

    public void resetStreakBonuses() {
        prefs.edit()
                .putBoolean(KEY_STREAK_3_AWARDED, false)
                .putBoolean(KEY_STREAK_7_AWARDED, false)
                .apply();
    }

    // ── Badges ──────────────────────────────────────────────────────────

    public Set<String> getEarnedBadges() {
        return new HashSet<>(prefs.getStringSet(KEY_EARNED_BADGES, new HashSet<>()));
    }

    public void addBadge(String badgeId) {
        Set<String> badges = getEarnedBadges();
        badges.add(badgeId);
        prefs.edit().putStringSet(KEY_EARNED_BADGES, badges).apply();
    }

    public boolean hasBadge(String badgeId) {
        return getEarnedBadges().contains(badgeId);
    }

    /**
     * Removes badge ids from the pre-shared-spec scheme so they don't linger in the
     * earned set after the schema change. New spec ids are kept untouched.
     */
    public void purgeLegacyBadges() {
        Set<String> badges = getEarnedBadges();
        boolean changed = badges.remove("first_steps");
        changed |= badges.remove("champion");
        changed |= badges.remove("on_fire");
        changed |= badges.remove("unstoppable");
        changed |= badges.remove("certified");
        changed |= badges.remove("polymath");
        if (changed) {
            prefs.edit().putStringSet(KEY_EARNED_BADGES, badges).apply();
        }
    }

    // ── Event counters ──────────────────────────────────────────────────

    public int getLessonsCompleted() {
        return prefs.getInt(KEY_LESSONS_COMPLETED, 0);
    }

    public int incrementLessonsCompleted() {
        int count = getLessonsCompleted() + 1;
        prefs.edit().putInt(KEY_LESSONS_COMPLETED, count).apply();
        trackLessonForToday();
        recordLessonDate();
        return count;
    }

    public int getCoursesEnrolled() {
        return prefs.getInt(KEY_COURSES_ENROLLED, 0);
    }

    public int incrementCoursesEnrolled() {
        int count = getCoursesEnrolled() + 1;
        prefs.edit().putInt(KEY_COURSES_ENROLLED, count).apply();
        return count;
    }

    public int getCoursesCompleted() {
        return prefs.getInt(KEY_COURSES_COMPLETED, 0);
    }

    public int incrementCoursesCompleted() {
        int count = getCoursesCompleted() + 1;
        prefs.edit().putInt(KEY_COURSES_COMPLETED, count).apply();
        return count;
    }

    public int getExamsPassed() {
        return prefs.getInt(KEY_EXAMS_PASSED, 0);
    }

    public int incrementExamsPassed() {
        int count = getExamsPassed() + 1;
        prefs.edit().putInt(KEY_EXAMS_PASSED, count).apply();
        return count;
    }

    public int getCertificatesEarned() {
        return prefs.getInt(KEY_CERTIFICATES_EARNED, 0);
    }

    public int incrementCertificatesEarned() {
        int count = getCertificatesEarned() + 1;
        prefs.edit().putInt(KEY_CERTIFICATES_EARNED, count).apply();
        return count;
    }

    // ── Speed-run / on-a-roll tracking ──────────────────────────────────

    /** Lessons completed today (resets when the calendar date changes). */
    public int getLessonsToday() {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String stored = prefs.getString(KEY_LESSONS_TODAY_DATE, "");
        if (!today.equals(stored)) return 0;
        return prefs.getInt(KEY_LESSONS_TODAY_COUNT, 0);
    }

    private void trackLessonForToday() {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String stored = prefs.getString(KEY_LESSONS_TODAY_DATE, "");
        int next = today.equals(stored) ? prefs.getInt(KEY_LESSONS_TODAY_COUNT, 0) + 1 : 1;
        prefs.edit()
                .putString(KEY_LESSONS_TODAY_DATE, today)
                .putInt(KEY_LESSONS_TODAY_COUNT, next)
                .apply();
    }

    /** Rolling 7-day lesson count (uses recorded lesson dates). */
    public int getLessonsLast7Days() {
        Set<String> dates = getLessonDates();
        LocalDate today = LocalDate.now();
        int count = 0;
        for (int i = 0; i < 7; i++) {
            String key = today.minusDays(i).format(DateTimeFormatter.ISO_LOCAL_DATE);
            if (dates.contains(key)) count++;
        }
        return count;
    }

    private Set<String> getLessonDates() {
        return new TreeSet<>(prefs.getStringSet(KEY_LESSON_DATES, new HashSet<>()));
    }

    private void recordLessonDate() {
        TreeSet<String> dates = new TreeSet<>(prefs.getStringSet(KEY_LESSON_DATES, new HashSet<>()));
        dates.add(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        while (dates.size() > LESSON_DATES_MAX) {
            dates.remove(dates.first());
        }
        prefs.edit().putStringSet(KEY_LESSON_DATES, dates).apply();
    }
}

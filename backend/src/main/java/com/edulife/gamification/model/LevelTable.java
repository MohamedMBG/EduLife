package com.edulife.gamification.model;

import java.util.List;

/**
 * Level thresholds and labels are part of the shared gamification spec. Web and
 * Android consume them only through this backend module — never duplicate the
 * arrays on a client.
 */
public final class LevelTable {

    public static final int MAX_LEVEL = 10;

    private static final int[] THRESHOLDS =
            {0, 250, 600, 1100, 1800, 2700, 3900, 5500, 7500, 10000};

    private static final List<String> NAMES = List.of(
            "Novice",
            "Curious",
            "Explorer",
            "Seeker",
            "Thinker",
            "Achiever",
            "Scholar",
            "Expert",
            "Sage",
            "Master"
    );

    private LevelTable() {
    }

    /** Binary search for the highest threshold that is &lt;= totalXp. Returns 1..MAX_LEVEL. */
    public static int levelFor(int totalXp) {
        if (totalXp < 0) {
            return 1;
        }
        int lo = 0;
        int hi = THRESHOLDS.length - 1;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (THRESHOLDS[mid] <= totalXp) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return lo + 1;
    }

    public static int xpForLevel(int level) {
        int clamped = Math.max(1, Math.min(MAX_LEVEL, level));
        return THRESHOLDS[clamped - 1];
    }

    public static String nameForLevel(int level) {
        int clamped = Math.max(1, Math.min(MAX_LEVEL, level));
        return NAMES.get(clamped - 1);
    }

    public static int[] thresholds() {
        return THRESHOLDS.clone();
    }

    public static List<String> names() {
        return NAMES;
    }
}

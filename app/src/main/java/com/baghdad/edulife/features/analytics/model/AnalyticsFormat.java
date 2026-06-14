package com.baghdad.edulife.features.analytics.model;

import java.util.Locale;

/**
 * Pure formatting helpers for analytics values. Kept free of Android dependencies so it is unit
 * testable on the host JVM (the project's only test harness is plain JUnit). Locale.US keeps the
 * decimal separator deterministic regardless of device locale, so a rate always reads "85.0%".
 */
public final class AnalyticsFormat {

    private AnalyticsFormat() {}

    /** Formats a percentage value (already 0–100 from the backend) with one decimal and a % sign. */
    public static String percent(double value) {
        return String.format(Locale.US, "%.1f%%", value);
    }

    /** Formats a count for display. Negative values are clamped to 0 since counts are never negative. */
    public static String count(long value) {
        return String.valueOf(Math.max(0L, value));
    }

    /** "passed / attempts" summary line; shows a neutral dash when there are no attempts yet. */
    public static String passedOfAttempts(long passed, long attempts) {
        if (attempts <= 0) {
            return "—";
        }
        return count(passed) + " / " + count(attempts);
    }
}

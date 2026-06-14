package com.baghdad.edulife.features.analytics;

import static org.junit.Assert.assertEquals;

import com.baghdad.edulife.features.analytics.model.AnalyticsFormat;

import org.junit.Test;

/**
 * Host-JVM unit tests for the pure analytics formatting helpers. These run with the project's
 * existing plain-JUnit harness (no Android/Robolectric dependency).
 */
public class AnalyticsFormatTest {

    @Test
    public void percent_oneDecimalWithLocaleStableSeparator() {
        // Locale.US is forced inside the formatter, so a dot is used regardless of device locale.
        assertEquals("85.0%", AnalyticsFormat.percent(85.0));
        assertEquals("0.0%", AnalyticsFormat.percent(0.0));
        assertEquals("33.3%", AnalyticsFormat.percent(33.33));
        assertEquals("100.0%", AnalyticsFormat.percent(100.0));
    }

    @Test
    public void count_clampsNegativeToZero() {
        assertEquals("0", AnalyticsFormat.count(0));
        assertEquals("42", AnalyticsFormat.count(42));
        // Counts are never negative; defend against a bad payload rather than showing "-1".
        assertEquals("0", AnalyticsFormat.count(-1));
    }

    @Test
    public void passedOfAttempts_dashWhenNoAttempts() {
        assertEquals("—", AnalyticsFormat.passedOfAttempts(0, 0));
        assertEquals("3 / 5", AnalyticsFormat.passedOfAttempts(3, 5));
        assertEquals("0 / 2", AnalyticsFormat.passedOfAttempts(0, 2));
    }
}

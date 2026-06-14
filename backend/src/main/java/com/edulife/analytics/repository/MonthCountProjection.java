package com.edulife.analytics.repository;

/**
 * Read-only projection for a monthly cohort/trend bucket. {@code month} is a 'YYYY-MM' label
 * produced by date_trunc + to_char so the bucket key is stable and timezone-independent enough
 * for display.
 */
public interface MonthCountProjection {
    String getMonth();
    long getTotal();
}

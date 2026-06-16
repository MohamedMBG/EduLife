package com.baghdad.edulife.features.analytics.model;

/**
 * One day in the weekly study-activity chart. {@code label} is a short weekday name (Mon…Sun) and
 * {@code lessonsCompleted} drives the bar height in {@link WeeklyStudyActivity}.
 */
public class DayStudyActivity {
    public final String label;
    public final int lessonsCompleted;
    /** True for the day matching "today" so the chart can highlight it. */
    public final boolean today;

    public DayStudyActivity(String label, int lessonsCompleted, boolean today) {
        this.label = label;
        this.lessonsCompleted = lessonsCompleted;
        this.today = today;
    }
}

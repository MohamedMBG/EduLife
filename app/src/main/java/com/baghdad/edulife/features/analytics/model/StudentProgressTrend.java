package com.baghdad.edulife.features.analytics.model;

import java.util.List;

/** Student's own lessons-completed-per-month trend. Mirrors backend StudentProgressTrendDto. */
public class StudentProgressTrend {
    public long totalLessons;
    public List<MonthCount> lessonsByMonth;
}

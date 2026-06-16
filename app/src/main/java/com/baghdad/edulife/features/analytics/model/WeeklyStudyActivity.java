package com.baghdad.edulife.features.analytics.model;

import java.util.List;

/**
 * Last-7-days study activity for the weekly bar chart. Ordered Mon→Sun. Built in the repository
 * by bucketing real lesson-completion timestamps from /progress/courses/{id} into the seven days
 * ending today.
 */
public class WeeklyStudyActivity {
    public final List<DayStudyActivity> days;
    public final int totalLessonsThisWeek;
    public final int daysStudiedThisWeek;

    public WeeklyStudyActivity(List<DayStudyActivity> days, int totalLessonsThisWeek,
                               int daysStudiedThisWeek) {
        this.days = days;
        this.totalLessonsThisWeek = totalLessonsThisWeek;
        this.daysStudiedThisWeek = daysStudiedThisWeek;
    }
}

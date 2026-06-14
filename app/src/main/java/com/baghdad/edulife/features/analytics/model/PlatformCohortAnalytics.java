package com.baghdad.edulife.features.analytics.model;

import java.util.List;

/** Global cohort analytics. Mirrors backend PlatformCohortAnalyticsDto. */
public class PlatformCohortAnalytics {
    public Funnel funnel;
    public List<MonthCount> enrollmentCohorts;
    public List<MonthCount> certificateTrend;
}

package com.baghdad.edulife.features.analytics.model;

import java.util.List;

/**
 * Teacher analytics response. Mirrors the backend TeacherAnalyticsDto. The list contains only
 * the requesting teacher's own courses — the backend builds the scope, the client just renders.
 */
public class TeacherAnalytics {
    public long totalCourses;
    public List<TeacherCourseAnalytics> courses;
}

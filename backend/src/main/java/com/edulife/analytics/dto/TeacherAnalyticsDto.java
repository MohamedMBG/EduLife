package com.edulife.analytics.dto;

import java.util.List;

/**
 * Teacher analytics response: only the requesting teacher's own courses. The list is built
 * from courses whose author id equals the resolved user id, so a teacher can never see another
 * teacher's course metrics.
 */
public record TeacherAnalyticsDto(
        long totalCourses,
        List<TeacherCourseAnalyticsDto> courses
) {}

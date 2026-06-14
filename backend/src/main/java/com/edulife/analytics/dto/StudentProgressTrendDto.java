package com.edulife.analytics.dto;

import java.util.List;

/**
 * A student's own progress trend: lessons completed per month. Scoped to the authenticated user
 * server-side; never includes other learners' data.
 */
public record StudentProgressTrendDto(
        long totalLessons,
        List<MonthCountDto> lessonsByMonth
) {}

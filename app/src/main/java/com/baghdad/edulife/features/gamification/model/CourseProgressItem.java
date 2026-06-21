package com.baghdad.edulife.features.gamification.model;

import com.baghdad.edulife.features.courses.model.CourseProgressSummary;
import com.baghdad.edulife.features.courses.model.EnrolledCourse;

public class CourseProgressItem {

    public final EnrolledCourse course;
    public final CourseProgressSummary progress;
    public final boolean failed;

    public CourseProgressItem(EnrolledCourse course, CourseProgressSummary progress, boolean failed) {
        this.course = course;
        this.progress = progress;
        this.failed = failed;
    }
}

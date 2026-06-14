package com.baghdad.edulife.features.courses.model;

public class CareerCourseRecommendation {
    public final CourseSummary course;
    public final int score;
    public final String reason;

    public CareerCourseRecommendation(CourseSummary course, int score, String reason) {
        this.course = course;
        this.score = score;
        this.reason = reason;
    }
}

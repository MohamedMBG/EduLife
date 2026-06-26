package com.baghdad.edulife.features.courses.model;

/**
 * A single course recommendation from the career advisor, pairing a course with a relevance score and explanation.
 */
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

package com.baghdad.edulife.features.courses.model;

/**
 * DTO representing a course the learner is enrolled in, combining enrollment metadata with course summary fields.
 */
public class EnrolledCourse {
    public String enrollmentId;
    public String courseId;
    public String slug;
    public String title;
    public String shortDescription;
    public String level;
    public String languageCode;
    public String imageUrl;
    public String enrolledAt;
}

package com.baghdad.edulife.features.courses.model;

/**
 * Request body DTO for enrolling the current learner in a course.
 */
public class EnrollRequest {
    public final String courseId;

    public EnrollRequest(String courseId) {
        this.courseId = courseId;
    }
}

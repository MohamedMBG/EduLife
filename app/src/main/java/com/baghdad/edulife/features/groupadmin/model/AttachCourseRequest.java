package com.baghdad.edulife.features.groupadmin.model;

/** Request body for POST /api/v1/groups/{groupId}/courses. */
public class AttachCourseRequest {
    public final String courseId;

    public AttachCourseRequest(String courseId) {
        this.courseId = courseId;
    }
}

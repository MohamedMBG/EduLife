package com.baghdad.edulife.features.courses.model;

/**
 * Backend GET /api/v1/courses/{courseId}/lessons/{lessonId} payload.
 * Holds the actual lesson content the in-app viewer renders instead of handing off to Chrome.
 */
public class LessonDetail {
    public String lessonId;
    public String courseId;
    public String sectionId;
    public String sectionTitle;
    public String title;
    public String summary;
    public String lessonType;
    public String contentUrl;
    public String contentBody;
    public Integer durationMinutes;
    public Integer displayOrder;
    public boolean preview;
    public boolean completed;
}

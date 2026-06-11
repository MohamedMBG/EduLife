package com.baghdad.edulife.features.teacher.model;

public class CreateLessonRequest {
    public String title;
    public String summary;
    public String lessonType;
    public Integer estimatedDurationMinutes;
    public int displayOrder;
    public boolean preview;
    public String contentUrl;
    public String contentBody;

    public CreateLessonRequest(String title, String summary, String lessonType,
                               Integer estimatedDurationMinutes, int displayOrder,
                               boolean preview, String contentUrl, String contentBody) {
        this.title = title;
        this.summary = summary;
        this.lessonType = lessonType;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.displayOrder = displayOrder;
        this.preview = preview;
        this.contentUrl = contentUrl;
        this.contentBody = contentBody;
    }
}

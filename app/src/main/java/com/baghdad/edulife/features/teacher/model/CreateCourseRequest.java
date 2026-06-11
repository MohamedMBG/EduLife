package com.baghdad.edulife.features.teacher.model;

public class CreateCourseRequest {
    public String title;
    public String shortDescription;
    public String description;
    public String languageCode;
    public String level;
    public String imageUrl;

    public CreateCourseRequest(String title, String shortDescription,
                               String description, String languageCode,
                               String level, String imageUrl) {
        this.title = title;
        this.shortDescription = shortDescription;
        this.description = description;
        this.languageCode = languageCode;
        this.level = level;
        this.imageUrl = imageUrl;
    }
}

package com.baghdad.edulife.features.teacher.model;

public class CmsCourse {
    public String id;
    public String slug;
    public String title;
    public String shortDescription;
    public String description;
    public String languageCode;
    public String level;
    public String imageUrl;
    public String status;
    public String publishedAt;
    public String createdByUserId;
    // Lets approval reviewers (group admins) see which teacher authored a draft course.
    public String createdByEmail;
    public String createdAt;
    public String updatedAt;
}

package com.baghdad.edulife.features.courses.model;

/**
 * Lightweight DTO representing a course in list views, carrying only the essential metadata for catalog display.
 */
public class CourseSummary {
    public String id;
    public String slug;
    public String title;
    public String shortDescription;
    public String level;
    public String languageCode;
    public String imageUrl;
    public String publishedAt;
}

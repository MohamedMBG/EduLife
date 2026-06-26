package com.baghdad.edulife.features.courses.model;

import java.util.List;

/**
 * DTO representing the full details of a course as returned by the backend, including metadata and section breakdown.
 */
public class CourseDetail {
    public String id;
    public String slug;
    public String title;
    public String shortDescription;
    public String description;
    public String level;
    public String languageCode;
    public String imageUrl;
    public String publishedAt;
    public List<CourseSection> sections;
}

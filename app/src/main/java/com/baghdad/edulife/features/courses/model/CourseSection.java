package com.baghdad.edulife.features.courses.model;

import java.util.List;

/**
 * DTO representing a section within a course, containing its title, description, display order, and child lessons.
 */
public class CourseSection {
    public String id;
    public String title;
    public String description;
    public int displayOrder;
    public List<LessonSummary> lessons;
}

package com.baghdad.edulife.features.courses.model;

/** Lightweight lesson DTO used in course section listings, containing only the metadata needed for list rows. */
public class LessonSummary {
    public String id;
    public String title;
    public String summary;
    public String lessonType;
    public int estimatedDurationMinutes;
    public int displayOrder;
    public boolean preview;
}

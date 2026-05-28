package com.baghdad.edulife.features.courses.model;

import java.util.List;

public class CourseProgress {
    public String courseId;
    public int completedLessons;
    public int totalLessons;
    public double percentComplete;
    public List<SectionProgress> sections;

    public static class SectionProgress {
        public String sectionId;
        public String title;
        public int displayOrder;
        public List<LessonProgress> lessons;
    }

    public static class LessonProgress {
        public String lessonId;
        public String title;
        public String lessonType;
        public Integer durationMinutes;
        public int displayOrder;
        public boolean preview;
        public boolean completed;
        public String completedAt;
    }
}

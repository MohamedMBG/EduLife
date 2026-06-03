package com.baghdad.edulife.features.courses.model;

import java.util.List;

public class CourseProgressResponse {
    public String courseId;
    public int completedLessons;
    public int totalLessons;
    public double percentComplete;
    public List<SectionProgress> sections;

    public static class SectionProgress {
        public String sectionId;
        public String title;
        public List<LessonProgress> lessons;
    }

    public static class LessonProgress {
        public String lessonId;
        public boolean completed;
    }
}

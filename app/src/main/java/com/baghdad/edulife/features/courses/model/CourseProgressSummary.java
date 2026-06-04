package com.baghdad.edulife.features.courses.model;

import java.util.List;

public class CourseProgressSummary {
    public String courseId;
    public int completedLessons;
    public int totalLessons;
    public double percentComplete;
    public List<SectionProgressSummary> sections;

    public static class SectionProgressSummary {
        public String sectionId;
        public String title;
        public int displayOrder;
        public List<LessonProgressSummary> lessons;
    }

    public static class LessonProgressSummary {
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

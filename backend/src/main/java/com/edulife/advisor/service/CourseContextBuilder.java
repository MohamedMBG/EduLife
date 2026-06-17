package com.edulife.advisor.service;

import com.edulife.advisor.dto.CourseContextDto;
import com.edulife.courses.entity.Course;
import com.edulife.courses.entity.CourseSection;
import com.edulife.courses.entity.Lesson;
import com.edulife.courses.model.CourseStatus;
import com.edulife.courses.repository.CourseSectionRepository;
import com.edulife.courses.repository.CourseRepository;
import com.edulife.courses.repository.LessonRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CourseContextBuilder {

    private final CourseRepository courseRepository;
    private final CourseSectionRepository sectionRepository;
    private final LessonRepository lessonRepository;

    public CourseContextBuilder(CourseRepository courseRepository,
                                CourseSectionRepository sectionRepository,
                                LessonRepository lessonRepository) {
        this.courseRepository = courseRepository;
        this.sectionRepository = sectionRepository;
        this.lessonRepository = lessonRepository;
    }

    public List<CourseContextDto> build(String goal) {
        return courseRepository.findAllByStatus(CourseStatus.PUBLISHED)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private CourseContextDto toDto(Course course) {
        List<String> lessonTitles = fetchLessonTitles(course.getId());
        return new CourseContextDto(
                course.getId(),
                course.getTitle(),
                course.getShortDescription(),
                course.getDescription(),
                course.getLevel(),
                course.getLanguageCode(),
                List.of(),
                lessonTitles
        );
    }

    private List<String> fetchLessonTitles(UUID courseId) {
        List<CourseSection> sections = sectionRepository.findAllByCourseIdOrderByDisplayOrderAsc(courseId);
        List<String> titles = new ArrayList<>();
        for (CourseSection section : sections) {
            List<Lesson> lessons = lessonRepository.findAllByCourseSectionIdOrderByDisplayOrderAsc(section.getId());
            for (Lesson lesson : lessons) {
                titles.add(lesson.getTitle());
            }
        }
        return titles;
    }
}

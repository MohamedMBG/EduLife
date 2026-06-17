package com.edulife.advisor.service;

import com.edulife.advisor.dto.CourseContextDto;
import com.edulife.courses.model.CourseStatus;
import com.edulife.courses.repository.CourseRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CourseContextBuilder {

    private final CourseRepository courseRepository;

    public CourseContextBuilder(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    public List<CourseContextDto> build(String goal) {
        return courseRepository.findAllByStatus(CourseStatus.PUBLISHED)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private CourseContextDto toDto(com.edulife.courses.entity.Course course) {
        return new CourseContextDto(
                course.getId(),
                course.getTitle(),
                course.getShortDescription(),
                course.getLevel(),
                course.getLanguageCode(),
                List.of()
        );
    }
}

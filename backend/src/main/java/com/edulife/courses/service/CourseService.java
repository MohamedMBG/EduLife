package com.edulife.courses.service;

import com.edulife.courses.dto.CourseListItemResponse;
import com.edulife.courses.entity.Course;
import com.edulife.courses.model.CourseStatus;
import com.edulife.courses.repository.CourseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {

    private static final int MAX_PAGE_SIZE = 50;

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Transactional(readOnly = true)
    public Page<CourseListItemResponse> listPublishedCourses(String level, int page, int size) {
        int clampedSize = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, clampedSize, Sort.by("publishedAt").descending());

        Page<Course> courses = (level != null && !level.isBlank())
                ? courseRepository.findAllByStatusAndLevel(CourseStatus.PUBLISHED, level.trim(), pageable)
                : courseRepository.findAllByStatus(CourseStatus.PUBLISHED, pageable);

        return courses.map(this::toListItem);
    }

    private CourseListItemResponse toListItem(Course course) {
        return new CourseListItemResponse(
                course.getId(),
                course.getSlug(),
                course.getTitle(),
                course.getShortDescription(),
                course.getLevel(),
                course.getLanguageCode(),
                course.getPublishedAt()
        );
    }
}

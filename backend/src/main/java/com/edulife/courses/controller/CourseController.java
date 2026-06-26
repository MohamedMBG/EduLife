package com.edulife.courses.controller;

import com.edulife.courses.dto.CourseDetailDto;
import com.edulife.courses.dto.CourseSummaryDto;
import java.util.UUID;
import com.edulife.courses.service.CourseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for public course discovery endpoints.
 */
@RestController
@RequestMapping("/api/v1/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    /**
     * Lists published courses with optional category filter and full-text search.
     *
     * @param category optional level/category filter
     * @param q        optional full-text search query (Postgres tsquery)
     * @param pageable pagination parameters (default size 20)
     * @return paginated course summaries
     */
    @GetMapping
    public Page<CourseSummaryDto> listCourses(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        // `q` triggers Postgres full-text search; `category` filters by level when `q` is absent.
        return courseService.getPublishedCourses(category, q, pageable);
    }

    /**
     * Returns full detail for a single published course, including sections and lessons.
     *
     * @param courseId the course UUID
     * @return course detail with nested sections and lesson summaries
     */
    @GetMapping("/{courseId}")
    public CourseDetailDto getCourseDetail(@PathVariable UUID courseId) {
        return courseService.getPublishedCourseDetail(courseId);
    }
}

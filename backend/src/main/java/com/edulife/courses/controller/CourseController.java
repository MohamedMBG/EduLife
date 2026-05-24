package com.edulife.courses.controller;

import com.edulife.courses.dto.CourseDetailDto;
import com.edulife.courses.dto.CourseSummaryDto;
import java.util.UUID;
import com.edulife.courses.service.CourseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    // The list endpoint stays intentionally thin so request parsing and HTTP wiring remain
    // separate from the discovery business rules that belong in CourseService.
    public Page<CourseSummaryDto> listCourses(
            @RequestParam(required = false) String category,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return courseService.getPublishedCourses(category, pageable);
    }

    @GetMapping("/{courseId}")
    // Detail lookup is UUID-based because the learner flow already uses backend IDs from the
    // list response and this avoids reopening slug-routing decisions during Sprint 2.
    public CourseDetailDto getCourseDetail(@PathVariable UUID courseId) {
        return courseService.getPublishedCourseDetail(courseId);
    }
}

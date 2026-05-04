package com.edulife.courses.controller;

import com.edulife.courses.dto.CourseListItemResponse;
import com.edulife.courses.service.CourseService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public Page<CourseListItemResponse> listCourses(
            @RequestParam(required = false) String level,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return courseService.listPublishedCourses(level, page, size);
    }
}

package com.edulife.progress.controller;

import com.edulife.progress.dto.CourseProgressDto;
import com.edulife.progress.service.ProgressService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/progress")
public class ProgressQueryController {

    private final ProgressService progressService;

    public ProgressQueryController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping("/courses/{courseId}")
    public CourseProgressDto getCourseProgress(@PathVariable UUID courseId) {
        return progressService.getCourseProgress(courseId);
    }
}

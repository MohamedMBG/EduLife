package com.edulife.courses.controller;

import com.edulife.courses.dto.LessonDetailDto;
import com.edulife.courses.service.LessonService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for lesson detail endpoints, scoped under a course.
 */
@RestController
@RequestMapping("/api/v1/courses/{courseId}/lessons")
public class LessonController {

    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    /**
     * Returns full lesson content. Requires enrollment unless the lesson is a preview.
     *
     * @param courseId the parent course UUID
     * @param lessonId the lesson UUID
     * @return lesson detail including content body/URL and completion status
     */
    @GetMapping("/{lessonId}")
    public LessonDetailDto getLessonDetail(
            @PathVariable UUID courseId,
            @PathVariable UUID lessonId) {
        return lessonService.getLessonDetail(courseId, lessonId);
    }
}

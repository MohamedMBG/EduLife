package com.edulife.courses.controller;

import com.edulife.courses.dto.LessonDetailDto;
import com.edulife.courses.service.LessonService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/lessons")
public class LessonController {

    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @GetMapping("/{lessonId}")
    public LessonDetailDto getLessonDetail(
            @PathVariable UUID courseId,
            @PathVariable UUID lessonId) {
        return lessonService.getLessonDetail(courseId, lessonId);
    }
}

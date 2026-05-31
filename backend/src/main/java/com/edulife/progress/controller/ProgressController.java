package com.edulife.progress.controller;

import com.edulife.progress.service.ProgressService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/courses/{courseId}")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @PostMapping("/lessons/{lessonId}/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markComplete(
            @PathVariable UUID courseId,
            @PathVariable UUID lessonId) {
        progressService.markLessonComplete(courseId, lessonId);
    }
}

package com.edulife.exams.controller;

import com.edulife.exams.dto.ExamDto;
import com.edulife.exams.dto.ExamResultDto;
import com.edulife.exams.dto.SubmitExamRequest;
import com.edulife.exams.service.ExamService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/exam")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @GetMapping
    public ExamDto getExam(@PathVariable UUID courseId) {
        return examService.getExam(courseId);
    }

    @PostMapping("/submit")
    public ExamResultDto submitExam(
            @PathVariable UUID courseId,
            @Valid @RequestBody SubmitExamRequest request) {
        return examService.submitExam(courseId, request);
    }
}

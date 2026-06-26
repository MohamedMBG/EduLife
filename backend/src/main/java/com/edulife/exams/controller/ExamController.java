package com.edulife.exams.controller;

import com.edulife.exams.dto.ExamDto;
import com.edulife.exams.dto.ExamResultDto;
import com.edulife.exams.dto.ExamStatusDto;
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

/**
 * REST controller for course exam operations including retrieval, status checks, and submission.
 */
@RestController
@RequestMapping("/api/v1/courses/{courseId}/exam")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    /** Returns the exam for a course with shuffled choices; requires active enrollment. */
    @GetMapping
    public ExamDto getExam(@PathVariable UUID courseId) {
        return examService.getExam(courseId);
    }

    /** Returns the learner's exam status including pass state, failed attempts, and cooldown info. */
    @GetMapping("/status")
    public ExamStatusDto getExamStatus(@PathVariable UUID courseId) {
        return examService.getExamStatus(courseId);
    }

    /** Submits exam answers for server-side scoring; issues a certificate on pass. */
    @PostMapping("/submit")
    public ExamResultDto submitExam(
            @PathVariable UUID courseId,
            @Valid @RequestBody SubmitExamRequest request) {
        return examService.submitExam(courseId, request);
    }
}

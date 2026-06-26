package com.edulife.admin.controller;

import com.edulife.admin.dto.CreateExamRequest;
import com.edulife.admin.dto.ExamAdminDto;
import com.edulife.admin.service.CmsExamService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for CMS exam authoring (CRUD operations on a course's exam). */
@RestController
@RequestMapping("/api/v1/cms/courses/{courseId}/exam")
@PreAuthorize("hasAnyRole('TEACHER','GROUP_ADMIN','ADMIN')")
public class CmsExamController {

    private final CmsExamService cmsExamService;

    public CmsExamController(CmsExamService cmsExamService) {
        this.cmsExamService = cmsExamService;
    }

    /**
     * Returns the exam with correct-answer flags for CMS review.
     * Unlike the learner endpoint, this never shuffles choices and always exposes isCorrect.
     * GET /api/v1/cms/courses/{courseId}/exam
     */
    @GetMapping
    public ExamAdminDto getExam(@PathVariable UUID courseId) {
        return cmsExamService.getExam(courseId);
    }

    /**
     * Creates the exam for a course atomically with all questions and choices.
     * Only one exam is allowed per course — a 409 is returned on duplicate.
     * POST /api/v1/cms/courses/{courseId}/exam
     */
    @PostMapping
    public ResponseEntity<ExamAdminDto> createExam(
            @PathVariable UUID courseId,
            @Valid @RequestBody CreateExamRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cmsExamService.createExam(courseId, request));
    }

    /** Replaces the exam's questions and choices wholesale (delete-and-recreate). */
    @PutMapping
    public ExamAdminDto updateExam(
            @PathVariable UUID courseId,
            @Valid @RequestBody CreateExamRequest request
    ) {
        return cmsExamService.updateExam(courseId, request);
    }

    /** Deletes the exam and all its questions and choices. */
    @DeleteMapping
    public ResponseEntity<Void> deleteExam(@PathVariable UUID courseId) {
        cmsExamService.deleteExam(courseId);
        return ResponseEntity.noContent().build();
    }
}

package com.edulife.admin.controller;

import com.edulife.admin.dto.CreateLessonRequest;
import com.edulife.admin.dto.LessonAdminDto;
import com.edulife.admin.dto.UpdateLessonRequest;
import com.edulife.admin.service.CmsLessonService;
import jakarta.validation.Valid;
import java.util.List;
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

@RestController
@RequestMapping("/api/v1/cms/sections/{sectionId}/lessons")
@PreAuthorize("hasAnyRole('TEACHER','GROUP_ADMIN','ADMIN')")
public class CmsLessonController {

    private final CmsLessonService cmsLessonService;

    public CmsLessonController(CmsLessonService cmsLessonService) {
        this.cmsLessonService = cmsLessonService;
    }

    /** GET /api/v1/cms/sections/{sectionId}/lessons */
    @GetMapping
    public List<LessonAdminDto> listLessons(@PathVariable UUID sectionId) {
        return cmsLessonService.listLessons(sectionId);
    }

    /** POST /api/v1/cms/sections/{sectionId}/lessons */
    @PostMapping
    public ResponseEntity<LessonAdminDto> createLesson(
            @PathVariable UUID sectionId,
            @Valid @RequestBody CreateLessonRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cmsLessonService.createLesson(sectionId, request));
    }

    /** PUT /api/v1/cms/sections/{sectionId}/lessons/{lessonId} */
    @PutMapping("/{lessonId}")
    public ResponseEntity<LessonAdminDto> updateLesson(
            @PathVariable UUID sectionId,
            @PathVariable UUID lessonId,
            @Valid @RequestBody UpdateLessonRequest request
    ) {
        return ResponseEntity.ok(cmsLessonService.updateLesson(sectionId, lessonId, request));
    }

    /** DELETE /api/v1/cms/sections/{sectionId}/lessons/{lessonId} */
    @DeleteMapping("/{lessonId}")
    public ResponseEntity<Void> deleteLesson(
            @PathVariable UUID sectionId,
            @PathVariable UUID lessonId
    ) {
        cmsLessonService.deleteLesson(sectionId, lessonId);
        return ResponseEntity.noContent().build();
    }
}

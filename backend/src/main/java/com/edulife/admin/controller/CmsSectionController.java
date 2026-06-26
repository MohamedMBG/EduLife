package com.edulife.admin.controller;

import com.edulife.admin.dto.CreateSectionRequest;
import com.edulife.admin.dto.SectionAdminDto;
import com.edulife.admin.dto.UpdateSectionRequest;
import com.edulife.admin.service.CmsSectionService;
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

/** REST controller for CMS section management within a course. */
@RestController
@RequestMapping("/api/v1/cms/courses/{courseId}/sections")
// Section management requires the same role gate as course management.
@PreAuthorize("hasAnyRole('TEACHER','GROUP_ADMIN','ADMIN')")
public class CmsSectionController {

    private final CmsSectionService cmsSectionService;

    public CmsSectionController(CmsSectionService cmsSectionService) {
        this.cmsSectionService = cmsSectionService;
    }

    /** GET /api/v1/cms/courses/{courseId}/sections — list sections for a course. */
    @GetMapping
    public List<SectionAdminDto> listSections(@PathVariable UUID courseId) {
        return cmsSectionService.listSections(courseId);
    }

    /** POST /api/v1/cms/courses/{courseId}/sections — add a new section. */
    @PostMapping
    public ResponseEntity<SectionAdminDto> createSection(
            @PathVariable UUID courseId,
            @Valid @RequestBody CreateSectionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cmsSectionService.createSection(courseId, request));
    }

    /** PUT /api/v1/cms/courses/{courseId}/sections/{sectionId} — update or reorder a section. */
    @PutMapping("/{sectionId}")
    public ResponseEntity<SectionAdminDto> updateSection(
            @PathVariable UUID courseId,
            @PathVariable UUID sectionId,
            @Valid @RequestBody UpdateSectionRequest request
    ) {
        return ResponseEntity.ok(cmsSectionService.updateSection(courseId, sectionId, request));
    }

    /** DELETE /api/v1/cms/courses/{courseId}/sections/{sectionId} — remove a section and its lessons. */
    @DeleteMapping("/{sectionId}")
    public ResponseEntity<Void> deleteSection(
            @PathVariable UUID courseId,
            @PathVariable UUID sectionId
    ) {
        cmsSectionService.deleteSection(courseId, sectionId);
        return ResponseEntity.noContent().build();
    }
}

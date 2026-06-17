package com.edulife.admin.controller;

import com.edulife.admin.dto.CourseAdminDto;
import com.edulife.admin.dto.CreateCourseRequest;
import com.edulife.admin.dto.UpdateCourseRequest;
import com.edulife.admin.service.CmsCourseService;
import com.edulife.courses.dto.CourseCoverUploadResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/cms/courses")
// TEACHER, GROUP_ADMIN, and ADMIN can create/edit courses; per-course ownership is enforced in the service layer.
@PreAuthorize("hasAnyRole('TEACHER','GROUP_ADMIN','ADMIN')")
public class CmsCourseController {

    private final CmsCourseService cmsCourseService;

    public CmsCourseController(CmsCourseService cmsCourseService) {
        this.cmsCourseService = cmsCourseService;
    }

    /**
     * Lists courses the caller owns (TEACHER), courses from managed teachers
     * (GROUP_ADMIN), or all upload requests including standalone teachers (ADMIN).
     * GET /api/v1/cms/courses
     */
    @GetMapping
    public List<CourseAdminDto> listMyCourses() {
        return cmsCourseService.listMyCourses();
    }

    /**
     * Creates a new course in DRAFT status. Slug is derived server-side.
     * POST /api/v1/cms/courses
     */
    @PostMapping
    public ResponseEntity<CourseAdminDto> createCourse(@Valid @RequestBody CreateCourseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cmsCourseService.createCourse(request));
    }

    /**
     * Updates mutable metadata of a DRAFT course. Status transitions happen via separate endpoints.
     * PUT /api/v1/cms/courses/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<CourseAdminDto> updateCourse(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCourseRequest request
    ) {
        return ResponseEntity.ok(cmsCourseService.updateCourse(id, request));
    }

    /**
     * Uploads or replaces the course cover image. TEACHER (owner), GROUP_ADMIN (manages author),
     * and ADMIN are permitted. Old cover is deleted from storage on replacement.
     * POST /api/v1/cms/courses/{id}/cover-image
     */
    @PostMapping(value = "/{id}/cover-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CourseCoverUploadResponse> uploadCoverImage(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(cmsCourseService.uploadCoverImage(id, file));
    }

    /**
     * Transitions a DRAFT course to PUBLISHED. Teachers cannot self-publish: ADMIN can
     * approve anything, GROUP_ADMIN only courses from teachers inside their groups
     * (membership check lives in the service). Standalone teachers stay in the
     * platform admin review queue.
     * PUT /api/v1/cms/courses/{id}/publish
     */
    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('GROUP_ADMIN','ADMIN')")
    public ResponseEntity<CourseAdminDto> publishCourse(@PathVariable UUID id) {
        return ResponseEntity.ok(cmsCourseService.publishCourse(id));
    }

    /**
     * Transitions a course to ARCHIVED. Learners can no longer discover or enroll.
     * PUT /api/v1/cms/courses/{id}/archive
     */
    @PutMapping("/{id}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseAdminDto> archiveCourse(@PathVariable UUID id) {
        return ResponseEntity.ok(cmsCourseService.archiveCourse(id));
    }
}

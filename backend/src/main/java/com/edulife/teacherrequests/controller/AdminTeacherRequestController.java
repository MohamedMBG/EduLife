package com.edulife.teacherrequests.controller;

import com.edulife.teacherrequests.dto.ReviewTeacherRequestRequest;
import com.edulife.teacherrequests.dto.TeacherRequestResponse;
import com.edulife.teacherrequests.model.RequestStatus;
import com.edulife.teacherrequests.service.TeacherRequestService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/teacher-requests")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTeacherRequestController {

    private final TeacherRequestService service;

    public AdminTeacherRequestController(TeacherRequestService service) {
        this.service = service;
    }

    @GetMapping
    public Page<TeacherRequestResponse> list(
            @RequestParam(required = false) RequestStatus status,
            @PageableDefault(size = 20, sort = "requestedAt") Pageable pageable
    ) {
        return service.listRequests(status, pageable);
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<TeacherRequestResponse> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(service.approve(id));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<TeacherRequestResponse> reject(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewTeacherRequestRequest body
    ) {
        return ResponseEntity.ok(service.reject(id, body));
    }
}

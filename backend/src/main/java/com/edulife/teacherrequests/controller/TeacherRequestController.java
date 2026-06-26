package com.edulife.teacherrequests.controller;

import com.edulife.teacherrequests.dto.SubmitTeacherRequestRequest;
import com.edulife.teacherrequests.dto.TeacherRequestResponse;
import com.edulife.teacherrequests.service.TeacherRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Learner-facing controller for submitting and checking the status of teacher promotion requests.
 */
@RestController
@RequestMapping("/api/v1/teacher-requests")
public class TeacherRequestController {

    private final TeacherRequestService service;

    public TeacherRequestController(TeacherRequestService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<TeacherRequestResponse> submit(
            @Valid @RequestBody SubmitTeacherRequestRequest body
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.submit(body));
    }

    @GetMapping("/me")
    public ResponseEntity<TeacherRequestResponse> getMyRequest() {
        return service.getMyLatestRequest()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}

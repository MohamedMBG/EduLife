package com.edulife.enrollments.controller;

import com.edulife.enrollments.dto.EnrolledCourseDto;
import com.edulife.enrollments.dto.EnrollRequest;
import com.edulife.enrollments.dto.EnrollmentResponse;
import com.edulife.enrollments.service.EnrollmentService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing course enrollments.
 */
@RestController
@RequestMapping("/api/v1/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    /** Enrolls the authenticated learner in a published course. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentResponse enroll(@RequestBody EnrollRequest request) {
        return enrollmentService.enroll(request.courseId());
    }

    /** Cancels an enrollment owned by the authenticated learner. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unenroll(@PathVariable UUID id) {
        enrollmentService.unenroll(id);
    }

    /** Returns all active enrollments for the authenticated learner, enriched with course details. */
    @GetMapping("/me")
    public List<EnrolledCourseDto> getMyEnrollments() {
        return enrollmentService.getMyEnrollments();
    }
}

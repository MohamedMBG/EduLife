package com.edulife.enrollments.controller;

import com.edulife.enrollments.dto.EnrolledCourseDto;
import com.edulife.enrollments.dto.EnrollRequest;
import com.edulife.enrollments.dto.EnrollmentResponse;
import com.edulife.enrollments.service.EnrollmentService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentResponse enroll(@RequestBody EnrollRequest request) {
        return enrollmentService.enroll(request.courseId());
    }

    @GetMapping
    public List<EnrolledCourseDto> getMyEnrollments() {
        return enrollmentService.getMyEnrollments();
    }
}

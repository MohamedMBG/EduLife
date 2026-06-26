package com.edulife.enrollments.dto;

import java.util.UUID;

/** Request body for enrolling a learner in a course. */
public record EnrollRequest(UUID courseId) {}

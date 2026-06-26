package com.edulife.groups.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Request payload to attach an existing course to a group. */
public record AttachCourseRequest(
        @NotNull UUID courseId
) {}

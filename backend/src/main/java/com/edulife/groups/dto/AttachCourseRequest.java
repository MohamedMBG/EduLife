package com.edulife.groups.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AttachCourseRequest(
        @NotNull UUID courseId
) {}

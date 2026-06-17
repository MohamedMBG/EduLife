package com.edulife.courses.dto;

import java.util.UUID;

public record CourseCoverUploadResponse(
        UUID courseId,
        String coverImageUrl,
        String coverImagePublicId,
        String message
) {
}

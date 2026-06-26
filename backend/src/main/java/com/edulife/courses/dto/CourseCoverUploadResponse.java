package com.edulife.courses.dto;

import java.util.UUID;

/** Response DTO returned after a successful course cover image upload. */
public record CourseCoverUploadResponse(
        UUID courseId,
        String coverImageUrl,
        String coverImagePublicId,
        String message
) {
}

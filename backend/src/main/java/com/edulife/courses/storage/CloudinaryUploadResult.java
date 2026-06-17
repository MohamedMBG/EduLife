package com.edulife.courses.storage;

public record CloudinaryUploadResult(
        String secureUrl,
        String publicId
) {
}

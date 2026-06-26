package com.edulife.courses.storage;

/** Holds the secure URL and public ID returned after a successful Cloudinary upload. */
public record CloudinaryUploadResult(
        String secureUrl,
        String publicId
) {
}

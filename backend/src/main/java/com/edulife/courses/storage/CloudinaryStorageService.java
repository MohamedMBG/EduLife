package com.edulife.courses.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.edulife.config.CloudinaryProperties;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CloudinaryStorageService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryStorageService.class);

    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/jpg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private static final long MAX_FILE_BYTES = 5L * 1024 * 1024;

    private final Cloudinary cloudinary;
    private final CloudinaryProperties properties;

    public CloudinaryStorageService(Cloudinary cloudinary, CloudinaryProperties properties) {
        this.cloudinary = cloudinary;
        this.properties = properties;
    }

    @SuppressWarnings("unchecked")
    public CloudinaryUploadResult store(UUID courseId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file is required");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Image must be smaller than 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.containsKey(contentType.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Please upload a JPG, PNG, or WebP image");
        }

        String folder = properties.getFolder() + "/course-covers";
        String publicId = courseId + "-" + UUID.randomUUID();

        try {
            Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", folder,
                    "public_id", publicId,
                    "overwrite", true,
                    "resource_type", "image"
            ));
            String secureUrl = (String) result.get("secure_url");
            String fullPublicId = (String) result.get("public_id");
            return new CloudinaryUploadResult(secureUrl, fullPublicId);
        } catch (IOException e) {
            log.error("Failed to upload course cover for course {}", courseId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cover image upload failed");
        }
    }

    public void deleteByPublicId(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
        } catch (IOException e) {
            log.warn("Failed to delete Cloudinary asset {}", publicId, e);
        }
    }
}

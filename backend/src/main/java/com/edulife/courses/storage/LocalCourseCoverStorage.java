package com.edulife.courses.storage;

import com.edulife.courses.config.CourseCoverStorageProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Component
public class LocalCourseCoverStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalCourseCoverStorage.class);

    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/jpg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final CourseCoverStorageProperties properties;

    public LocalCourseCoverStorage(CourseCoverStorageProperties properties) {
        this.properties = properties;
    }

    public String store(UUID courseId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file is required");
        }
        if (file.getSize() > properties.getMaxFileBytes()) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Image must be smaller than 5MB");
        }
        String contentType = file.getContentType();
        String extension = contentType == null ? null : ALLOWED_TYPES.get(contentType.toLowerCase());
        if (extension == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Please upload a JPG, PNG, or WebP image");
        }

        Path baseDir = Paths.get(properties.getStorageDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(baseDir);
            String filename = courseId + "-" + UUID.randomUUID() + "." + extension;
            Path target = baseDir.resolve(filename).normalize();
            if (!target.startsWith(baseDir)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid target path");
            }
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return buildPublicUrl(filename);
        } catch (IOException e) {
            log.error("Failed to write course cover for course {}", courseId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cover image write failed");
        }
    }

    public void deleteIfStored(String publicUrl) {
        if (publicUrl == null || publicUrl.isBlank()) {
            return;
        }
        String base = trimTrailingSlash(properties.getPublicBaseUrl());
        if (!publicUrl.startsWith(base + "/")) {
            return;
        }
        String filename = publicUrl.substring(base.length() + 1);
        Path baseDir = Paths.get(properties.getStorageDir()).toAbsolutePath().normalize();
        Path target = baseDir.resolve(filename).normalize();
        if (!target.startsWith(baseDir)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("Failed to delete previous course cover {}", filename, e);
        }
    }

    private String buildPublicUrl(String filename) {
        return trimTrailingSlash(properties.getPublicBaseUrl()) + "/" + filename;
    }

    private static String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}

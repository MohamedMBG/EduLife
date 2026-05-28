package com.edulife.profiles.storage;

import com.edulife.profiles.config.AvatarStorageProperties;
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
public class LocalAvatarStorage implements AvatarStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalAvatarStorage.class);

    // Filename is generated from a UUID and a fixed extension whitelist so a malicious client
    // cannot smuggle a script path or unexpected MIME through the original filename.
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/jpg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final AvatarStorageProperties properties;

    public LocalAvatarStorage(AvatarStorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public StoredAvatar store(UUID userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Avatar file is required");
        }
        if (file.getSize() > properties.getMaxFileBytes()) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Avatar exceeds " + properties.getMaxFileBytes() + " bytes");
        }
        String contentType = file.getContentType();
        String extension = contentType == null ? null : ALLOWED_TYPES.get(contentType.toLowerCase());
        if (extension == null) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Avatar must be image/jpeg, image/png, or image/webp");
        }

        Path baseDir = Paths.get(properties.getStorageDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(baseDir);
            String filename = userId + "-" + UUID.randomUUID() + "." + extension;
            Path target = baseDir.resolve(filename).normalize();
            // Defence in depth against path traversal: the resolved path must remain inside baseDir.
            if (!target.startsWith(baseDir)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid avatar target path");
            }
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredAvatar(buildPublicUrl(filename));
        } catch (IOException e) {
            log.error("Failed to write avatar for user {}", userId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Avatar write failed");
        }
    }

    @Override
    public void deleteIfStored(String publicUrl) {
        if (publicUrl == null || publicUrl.isBlank()) {
            return;
        }
        String base = trimTrailingSlash(properties.getPublicBaseUrl());
        if (!publicUrl.startsWith(base + "/")) {
            // URL points elsewhere (e.g. legacy CDN); leave it alone.
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
            // Stale avatar is not worth failing the request; log and continue.
            log.warn("Failed to delete previous avatar {}", filename, e);
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

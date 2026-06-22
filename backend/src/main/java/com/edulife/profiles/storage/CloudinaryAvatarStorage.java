package com.edulife.profiles.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.edulife.config.CloudinaryProperties;
import com.edulife.profiles.config.AvatarStorageProperties;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@Primary
public class CloudinaryAvatarStorage implements AvatarStorage {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryAvatarStorage.class);

    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/jpg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final Cloudinary cloudinary;
    private final CloudinaryProperties cloudinaryProperties;
    private final AvatarStorageProperties avatarProperties;

    public CloudinaryAvatarStorage(
            Cloudinary cloudinary,
            CloudinaryProperties cloudinaryProperties,
            AvatarStorageProperties avatarProperties) {
        this.cloudinary = cloudinary;
        this.cloudinaryProperties = cloudinaryProperties;
        this.avatarProperties = avatarProperties;
    }

    @Override
    @SuppressWarnings("unchecked")
    public StoredAvatar store(UUID userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Avatar file is required");
        }
        if (file.getSize() > avatarProperties.getMaxFileBytes()) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Avatar exceeds " + avatarProperties.getMaxFileBytes() + " bytes");
        }
        String contentType = file.getContentType();
        String declaredExtension = contentType == null ? null : ALLOWED_TYPES.get(contentType.toLowerCase());
        if (declaredExtension == null) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Avatar must be image/jpeg, image/png, or image/webp");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            log.error("Failed to read avatar upload for user {}", userId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Avatar read failed");
        }
        String extension = detectImageExtension(bytes);
        if (extension == null) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Avatar content is not a valid JPEG, PNG, or WebP image");
        }

        String folder = cloudinaryProperties.getFolder() + "/avatars";
        String publicId = userId + "-" + UUID.randomUUID();
        try {
            Map<String, Object> result = cloudinary.uploader().upload(bytes, ObjectUtils.asMap(
                    "folder", folder,
                    "public_id", publicId,
                    "overwrite", true,
                    "resource_type", "image"
            ));
            String secureUrl = (String) result.get("secure_url");
            String fullPublicId = (String) result.get("public_id");
            if (secureUrl == null || secureUrl.isBlank() || fullPublicId == null || fullPublicId.isBlank()) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Avatar upload failed");
            }
            return new StoredAvatar(secureUrl);
        } catch (IOException e) {
            log.error("Failed to upload avatar for user {}", userId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Avatar upload failed");
        }
    }

    @Override
    public void deleteIfStored(String publicUrl) {
        String publicId = extractPublicId(publicUrl);
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
        } catch (IOException e) {
            // Stale avatar is not worth failing the request; log and continue.
            log.warn("Failed to delete Cloudinary avatar {}", publicId, e);
        }
    }

    private String extractPublicId(String publicUrl) {
        if (publicUrl == null || publicUrl.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(publicUrl.trim());
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return null;
            }
            String marker = "/upload/";
            int markerIndex = path.indexOf(marker);
            if (markerIndex < 0) {
                return null;
            }
            String tail = path.substring(markerIndex + marker.length());
            // Cloudinary versioned URLs look like v123/...; strip that prefix before deriving the
            // public id so we only destroy assets from the expected avatar folder.
            if (tail.startsWith("v")) {
                int nextSlash = tail.indexOf('/');
                if (nextSlash >= 0) {
                    tail = tail.substring(nextSlash + 1);
                }
            }
            int dotIndex = tail.lastIndexOf('.');
            if (dotIndex > 0) {
                tail = tail.substring(0, dotIndex);
            }
            String expectedFolder = cloudinaryProperties.getFolder() + "/avatars/";
            return tail.startsWith(expectedFolder) ? tail : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String detectImageExtension(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'
                && (bytes[4] & 0xFF) == 0x0D && (bytes[5] & 0xFF) == 0x0A
                && (bytes[6] & 0xFF) == 0x1A && (bytes[7] & 0xFF) == 0x0A) {
            return "png";
        }
        if (bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "webp";
        }
        return null;
    }
}

package com.edulife.profiles.storage;

import com.edulife.profiles.config.AvatarStorageProperties;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Filesystem-backed {@link AvatarStorage} implementation for local development.
 * Writes avatars to the configured directory and serves them via the static resource mapping.
 */
@Component
public class LocalAvatarStorage implements AvatarStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalAvatarStorage.class);
    private static final String AVATAR_PUBLIC_PATH = "/uploads/avatars";
    private static final Set<String> LOCAL_HOSTS = Set.of(
            "localhost",
            "127.0.0.1",
            "0.0.0.0",
            "::1",
            "[::1]"
    );

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
        String declaredExtension = contentType == null ? null : ALLOWED_TYPES.get(contentType.toLowerCase());
        if (declaredExtension == null) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Avatar must be image/jpeg, image/png, or image/webp");
        }

        // The Content-Type header is client-controlled and trivially spoofed, so the file bytes
        // are read once and the real format is derived from the magic bytes. The image is rejected
        // if the sniffed format does not match a supported type — this blocks a renamed script or
        // HTML file from being stored under an image extension and later served back to a browser.
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

        Path baseDir = Paths.get(properties.getStorageDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(baseDir);
            String filename = userId + "-" + UUID.randomUUID() + "." + extension;
            Path target = baseDir.resolve(filename).normalize();
            // Defence in depth against path traversal: the resolved path must remain inside baseDir.
            if (!target.startsWith(baseDir)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid avatar target path");
            }
            Files.write(target, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return new StoredAvatar(buildPublicUrl(filename));
        } catch (IOException e) {
            log.error("Failed to write avatar for user {}", userId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Avatar write failed");
        }
    }

    /**
     * Returns the file extension for a supported image format detected from its magic bytes,
     * or {@code null} if the leading bytes match no supported format. Detection never trusts the
     * declared Content-Type or filename.
     */
    private static String detectImageExtension(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        // JPEG: FF D8 FF
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'
                && (bytes[4] & 0xFF) == 0x0D && (bytes[5] & 0xFF) == 0x0A
                && (bytes[6] & 0xFF) == 0x1A && (bytes[7] & 0xFF) == 0x0A) {
            return "png";
        }
        // WebP: "RIFF" .... "WEBP"
        if (bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "webp";
        }
        return null;
    }

    @Override
    public void deleteIfStored(String publicUrl) {
        if (publicUrl == null || publicUrl.isBlank()) {
            return;
        }
        String filename = extractStoredFilename(publicUrl);
        if (filename == null || filename.isBlank()) {
            // URL points elsewhere (for example a CDN-hosted legacy avatar); leave it alone.
            return;
        }
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

    /** Builds the publicly accessible avatar URL, preferring the request origin over a localhost fallback. */
    private String buildPublicUrl(String filename) {
        String configuredBaseUrl = trimTrailingSlash(properties.getPublicBaseUrl());
        String requestBaseUrl = resolveRequestPublicBaseUrl();

        // Render and other proxies terminate TLS in front of Spring. When the configured public
        // base URL is still the localhost fallback, prefer the request origin so API responses do
        // not hand the website a CSP-blocked localhost avatar URL after upload.
        String baseUrl = shouldUseRequestOrigin(configuredBaseUrl, requestBaseUrl)
                ? requestBaseUrl
                : configuredBaseUrl;
        return trimTrailingSlash(baseUrl) + "/" + filename;
    }

    private String resolveRequestPublicBaseUrl() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes)) {
            return null;
        }
        return trimTrailingSlash(ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(AVATAR_PUBLIC_PATH)
                .build()
                .toUriString());
    }

    private static boolean shouldUseRequestOrigin(String configuredBaseUrl, String requestBaseUrl) {
        return requestBaseUrl != null && !requestBaseUrl.isBlank()
                && (configuredBaseUrl.isBlank() || isLocalFallbackUrl(configuredBaseUrl));
    }

    private static boolean isLocalFallbackUrl(String url) {
        try {
            String host = new URI(url).getHost();
            return host != null && LOCAL_HOSTS.contains(host);
        } catch (Exception ignored) {
            return false;
        }
    }

    /** Extracts the filename from a locally-stored avatar URL, or {@code null} if the URL is external. */
    private static String extractStoredFilename(String publicUrl) {
        String normalized = publicUrl.trim();
        if (normalized.isBlank()) {
            return null;
        }

        String path = normalized;
        try {
            URI parsed = new URI(normalized);
            if (parsed.getPath() != null && !parsed.getPath().isBlank()) {
                path = parsed.getPath();
            }
        } catch (Exception ignored) {
            // Keep the raw value; relative paths still flow through the fixed-prefix check below.
        }

        String pathPrefix = AVATAR_PUBLIC_PATH + "/";
        int prefixIndex = path.indexOf(pathPrefix);
        if (prefixIndex < 0) {
            return null;
        }

        String filename = path.substring(prefixIndex + pathPrefix.length());
        if (filename.isBlank() || filename.contains("/")) {
            return null;
        }
        return filename;
    }

    private static String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}

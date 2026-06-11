package com.edulife.config;

import com.edulife.certificates.config.CertificateStorageProperties;
import com.edulife.profiles.config.AvatarStorageProperties;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Validates that both storage directories (avatar uploads and certificate PDFs) exist and are
 * writable before the application accepts traffic.  Failing early with a clear error is
 * preferable to silently failing on the first upload or certificate generation request.
 */
@Component
public class StorageReadinessCheck {

    private static final Logger log = LoggerFactory.getLogger(StorageReadinessCheck.class);

    private final AvatarStorageProperties avatarProperties;
    private final CertificateStorageProperties certProperties;

    public StorageReadinessCheck(
            AvatarStorageProperties avatarProperties,
            CertificateStorageProperties certProperties) {
        this.avatarProperties = avatarProperties;
        this.certProperties = certProperties;
    }

    @PostConstruct
    public void validate() {
        ensureWritable("avatar", avatarProperties.getStorageDir());
        ensureWritable("certificate", certProperties.getStorageDir());
    }

    private void ensureWritable(String label, String rawPath) {
        Path dir = Path.of(rawPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Cannot create " + label + " storage directory: " + dir
                    + " — check the corresponding env var and filesystem permissions.", e);
        }
        if (!Files.isWritable(dir)) {
            throw new IllegalStateException(
                    label + " storage directory is not writable: " + dir
                    + " — the process user must have write permission.");
        }
        log.info("{} storage directory ready: {}", label, dir);
    }
}

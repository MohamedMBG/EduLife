package com.edulife.profiles.storage;

import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

/** Strategy interface for storing and deleting user avatar images. */
public interface AvatarStorage {

    /** Validates and persists the uploaded file, returning its public URL. */
    StoredAvatar store(UUID userId, MultipartFile file);

    /** Deletes the avatar at the given URL if it was managed by this storage; no-op otherwise. */
    void deleteIfStored(String publicUrl);

    /** Result of a successful avatar store operation. */
    record StoredAvatar(String publicUrl) {}
}

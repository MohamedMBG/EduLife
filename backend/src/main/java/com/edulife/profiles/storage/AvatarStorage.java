package com.edulife.profiles.storage;

import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface AvatarStorage {

    StoredAvatar store(UUID userId, MultipartFile file);

    void deleteIfStored(String publicUrl);

    record StoredAvatar(String publicUrl) {}
}

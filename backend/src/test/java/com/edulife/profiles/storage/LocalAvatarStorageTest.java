package com.edulife.profiles.storage;

import com.edulife.profiles.config.AvatarStorageProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalAvatarStorageTest {

    @Test
    void storeWritesFileAndReturnsPublicUrl(@TempDir Path tempDir) throws Exception {
        LocalAvatarStorage storage = newStorage(tempDir);

        AvatarStorage.StoredAvatar stored = storage.store(
                UUID.randomUUID(),
                new MockMultipartFile("file", "a.png", "image/png", new byte[] {1, 2, 3})
        );

        assertThat(stored.publicUrl()).startsWith("http://localhost:8080/uploads/avatars/");
        assertThat(stored.publicUrl()).endsWith(".png");

        String filename = stored.publicUrl().substring(stored.publicUrl().lastIndexOf('/') + 1);
        assertThat(Files.exists(tempDir.resolve(filename))).isTrue();
    }

    @Test
    void storeRejectsEmptyFile(@TempDir Path tempDir) {
        LocalAvatarStorage storage = newStorage(tempDir);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                storage.store(UUID.randomUUID(),
                        new MockMultipartFile("file", "a.png", "image/png", new byte[0]))
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void storeRejectsTooLargeFile(@TempDir Path tempDir) {
        AvatarStorageProperties props = props(tempDir);
        props.setMaxFileBytes(8);
        LocalAvatarStorage storage = new LocalAvatarStorage(props);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                storage.store(UUID.randomUUID(),
                        new MockMultipartFile("file", "a.png", "image/png", new byte[16]))
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(413);
    }

    @Test
    void storeRejectsUnsupportedMediaType(@TempDir Path tempDir) {
        LocalAvatarStorage storage = newStorage(tempDir);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                storage.store(UUID.randomUUID(),
                        new MockMultipartFile("file", "a.gif", "image/gif", new byte[] {1, 2}))
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(415);
    }

    @Test
    void deleteIfStoredRemovesPreviousFile(@TempDir Path tempDir) throws Exception {
        LocalAvatarStorage storage = newStorage(tempDir);

        AvatarStorage.StoredAvatar stored = storage.store(
                UUID.randomUUID(),
                new MockMultipartFile("file", "a.png", "image/png", new byte[] {1, 2})
        );

        storage.deleteIfStored(stored.publicUrl());

        String filename = stored.publicUrl().substring(stored.publicUrl().lastIndexOf('/') + 1);
        assertThat(Files.exists(tempDir.resolve(filename))).isFalse();
    }

    @Test
    void deleteIfStoredIgnoresExternalUrl(@TempDir Path tempDir) {
        LocalAvatarStorage storage = newStorage(tempDir);

        // Should not raise even though the URL points outside the local store.
        storage.deleteIfStored("https://cdn.example.com/legacy/avatar.png");
    }

    private static LocalAvatarStorage newStorage(Path tempDir) {
        return new LocalAvatarStorage(props(tempDir));
    }

    private static AvatarStorageProperties props(Path tempDir) {
        AvatarStorageProperties props = new AvatarStorageProperties();
        props.setStorageDir(tempDir.toString());
        props.setPublicBaseUrl("http://localhost:8080/uploads/avatars");
        return props;
    }
}

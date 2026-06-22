package com.edulife.profiles.storage;

import com.edulife.profiles.config.AvatarStorageProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalAvatarStorageTest {

    // Minimal valid PNG signature so magic-byte sniffing accepts the upload.
    private static final byte[] PNG_BYTES =
            {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

    @Test
    void storeWritesFileAndReturnsPublicUrl(@TempDir Path tempDir) throws Exception {
        LocalAvatarStorage storage = newStorage(tempDir);

        AvatarStorage.StoredAvatar stored = storage.store(
                UUID.randomUUID(),
                new MockMultipartFile("file", "a.png", "image/png", PNG_BYTES)
        );

        assertThat(stored.publicUrl()).startsWith("http://localhost:8080/uploads/avatars/");
        assertThat(stored.publicUrl()).endsWith(".png");

        String filename = stored.publicUrl().substring(stored.publicUrl().lastIndexOf('/') + 1);
        assertThat(Files.exists(tempDir.resolve(filename))).isTrue();
    }

    @Test
    void storePrefersForwardedRequestOriginWhenConfiguredBaseIsLocalhostFallback(@TempDir Path tempDir) {
        LocalAvatarStorage storage = newStorage(tempDir);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/profile/avatar");
        request.setScheme("https");
        request.setServerName("edulife-2bro.onrender.com");
        request.setServerPort(443);

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            AvatarStorage.StoredAvatar stored = storage.store(
                    UUID.randomUUID(),
                    new MockMultipartFile("file", "a.png", "image/png", PNG_BYTES)
            );

            assertThat(stored.publicUrl()).startsWith("https://edulife-2bro.onrender.com/uploads/avatars/");
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
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
    void storeRejectsSpoofedContentType(@TempDir Path tempDir) {
        LocalAvatarStorage storage = newStorage(tempDir);

        // Declares image/png but the bytes are HTML — magic-byte sniffing must reject it so a
        // renamed script cannot be stored and served back to a browser as an avatar.
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                storage.store(UUID.randomUUID(),
                        new MockMultipartFile("file", "x.png", "image/png",
                                "<html><script>alert(1)</script></html>".getBytes()))
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(415);
    }

    @Test
    void deleteIfStoredRemovesPreviousFile(@TempDir Path tempDir) throws Exception {
        LocalAvatarStorage storage = newStorage(tempDir);

        AvatarStorage.StoredAvatar stored = storage.store(
                UUID.randomUUID(),
                new MockMultipartFile("file", "a.png", "image/png", PNG_BYTES)
        );

        storage.deleteIfStored(stored.publicUrl());

        String filename = stored.publicUrl().substring(stored.publicUrl().lastIndexOf('/') + 1);
        assertThat(Files.exists(tempDir.resolve(filename))).isFalse();
    }

    @Test
    void deleteIfStoredRemovesFileGeneratedFromDifferentPublicOrigin(@TempDir Path tempDir) throws Exception {
        LocalAvatarStorage storage = newStorage(tempDir);

        AvatarStorage.StoredAvatar stored = storage.store(
                UUID.randomUUID(),
                new MockMultipartFile("file", "a.png", "image/png", PNG_BYTES)
        );

        String filename = stored.publicUrl().substring(stored.publicUrl().lastIndexOf('/') + 1);
        storage.deleteIfStored("https://edulife-2bro.onrender.com/uploads/avatars/" + filename);

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

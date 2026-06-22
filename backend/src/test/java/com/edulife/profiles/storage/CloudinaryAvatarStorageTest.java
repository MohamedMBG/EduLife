package com.edulife.profiles.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.edulife.config.CloudinaryProperties;
import com.edulife.profiles.config.AvatarStorageProperties;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class CloudinaryAvatarStorageTest {

    // Minimal valid PNG signature so magic-byte sniffing accepts the upload.
    private static final byte[] PNG_BYTES =
            {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

    @Test
    void storeUploadsAvatarToCloudinary() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        given(cloudinary.uploader()).willReturn(uploader);
        given(uploader.upload(any(byte[].class), anyMap())).willReturn(Map.of(
                "secure_url", "https://res.cloudinary.com/demo/image/upload/v123/edulife/avatars/a.png",
                "public_id", "edulife/avatars/a"
        ));

        CloudinaryAvatarStorage storage = newStorage(cloudinary);
        AvatarStorage.StoredAvatar stored = storage.store(
                UUID.randomUUID(),
                new MockMultipartFile("file", "a.png", "image/png", PNG_BYTES)
        );

        assertThat(stored.publicUrl())
                .isEqualTo("https://res.cloudinary.com/demo/image/upload/v123/edulife/avatars/a.png");
        verify(uploader).upload(any(byte[].class), anyMap());
    }

    @Test
    void storeRejectsSpoofedContentType() {
        Cloudinary cloudinary = mock(Cloudinary.class);
        CloudinaryAvatarStorage storage = newStorage(cloudinary);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                storage.store(UUID.randomUUID(),
                        new MockMultipartFile("file", "x.png", "image/png",
                                "<html><script>alert(1)</script></html>".getBytes()))
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(415);
        verifyNoInteractions(cloudinary);
    }

    @Test
    void deleteIfStoredDeletesOnlyAvatarAssetsFromConfiguredFolder() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        given(cloudinary.uploader()).willReturn(uploader);

        CloudinaryAvatarStorage storage = newStorage(cloudinary);
        storage.deleteIfStored("https://res.cloudinary.com/demo/image/upload/v123/edulife/avatars/a.png");

        verify(uploader).destroy(eq("edulife/avatars/a"), anyMap());
    }

    @Test
    void deleteIfStoredIgnoresForeignUrls() throws IOException {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        given(cloudinary.uploader()).willReturn(uploader);

        CloudinaryAvatarStorage storage = newStorage(cloudinary);
        storage.deleteIfStored("https://cdn.example.com/legacy/avatar.png");

        verifyNoInteractions(uploader);
    }

    private static CloudinaryAvatarStorage newStorage(Cloudinary cloudinary) {
        CloudinaryProperties cloudinaryProperties = new CloudinaryProperties();
        cloudinaryProperties.setFolder("edulife");

        AvatarStorageProperties avatarProperties = new AvatarStorageProperties();
        avatarProperties.setMaxFileBytes(5L * 1024L * 1024L);

        return new CloudinaryAvatarStorage(cloudinary, cloudinaryProperties, avatarProperties);
    }
}

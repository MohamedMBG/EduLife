package com.edulife.profiles;

import com.edulife.common.error.ApiErrorWriter;
import com.edulife.common.error.GlobalApiExceptionHandler;
import com.edulife.profiles.controller.ProfileController;
import com.edulife.profiles.dto.AvatarUploadResponse;
import com.edulife.profiles.service.ProfileService;
import com.edulife.security.SecurityConfig;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfileController.class)
@Import({SecurityConfig.class, ApiErrorWriter.class, GlobalApiExceptionHandler.class})
class ProfileAvatarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProfileService profileService;

    @MockBean
    private FirebaseAuth firebaseAuth;

    @Test
    void uploadAvatarReturnsPublicUrlOnSuccess() throws Exception {
        mockValidFirebaseToken();

        given(profileService.uploadAvatar(any()))
                .willReturn(new AvatarUploadResponse("http://localhost:8080/uploads/avatars/abc.png"));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[] {1, 2, 3, 4}
        );

        mockMvc.perform(multipart("/api/v1/profile/avatar")
                        .file(file)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("http://localhost:8080/uploads/avatars/abc.png"));
    }

    @Test
    void uploadAvatarRejectsUnsupportedMediaType() throws Exception {
        mockValidFirebaseToken();

        given(profileService.uploadAvatar(any()))
                .willThrow(new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "Avatar must be image/jpeg, image/png, or image/webp"));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.gif",
                "image/gif",
                new byte[] {1, 2, 3}
        );

        mockMvc.perform(multipart("/api/v1/profile/avatar")
                        .file(file)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.message")
                        .value("Avatar must be image/jpeg, image/png, or image/webp"));
    }

    @Test
    void uploadAvatarRejectsEmptyFile() throws Exception {
        mockValidFirebaseToken();

        given(profileService.uploadAvatar(any()))
                .willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Avatar file is required"));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[0]
        );

        mockMvc.perform(multipart("/api/v1/profile/avatar")
                        .file(file)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Avatar file is required"));
    }

    @Test
    void uploadAvatarRequiresAuthentication() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[] {1, 2, 3}
        );

        mockMvc.perform(multipart("/api/v1/profile/avatar").file(file))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));

        verifyNoInteractions(profileService);
    }

    private void mockValidFirebaseToken() throws Exception {
        FirebaseToken decodedToken = org.mockito.Mockito.mock(FirebaseToken.class);
        given(firebaseAuth.verifyIdToken("valid-token")).willReturn(decodedToken);
        given(decodedToken.getUid()).willReturn("firebase-uid-123");
        given(decodedToken.getEmail()).willReturn("student@edulife.test");
        given(decodedToken.isEmailVerified()).willReturn(true);
    }
}

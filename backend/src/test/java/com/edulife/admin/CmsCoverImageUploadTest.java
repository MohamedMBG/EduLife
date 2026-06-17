package com.edulife.admin;

import com.edulife.admin.controller.CmsCourseController;
import com.edulife.admin.service.CmsCourseService;
import com.edulife.common.error.ApiErrorWriter;
import com.edulife.common.error.GlobalApiExceptionHandler;
import com.edulife.courses.dto.CourseCoverUploadResponse;
import com.edulife.security.SecurityConfig;
import com.edulife.users.entity.User;
import com.edulife.users.model.UserRole;
import com.edulife.users.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import java.util.Optional;
import java.util.UUID;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CmsCourseController.class)
@Import({SecurityConfig.class, ApiErrorWriter.class, GlobalApiExceptionHandler.class})
class CmsCoverImageUploadTest {

    private static final UUID COURSE_ID = UUID.fromString("cccccccc-4444-4444-4444-cccccccccccc");
    private static final UUID USER_ID = UUID.fromString("bbbbbbbb-3333-3333-3333-bbbbbbbbbbbb");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CmsCourseService cmsCourseService;

    @MockBean
    private FirebaseAuth firebaseAuth;

    @MockBean
    private UserRepository userRepository;

    @Test
    void teacherOwnerUploadReturnsCloudinaryUrl() throws Exception {
        mockValidFirebaseToken();
        mockUserRole(UserRole.TEACHER);

        given(cmsCourseService.uploadCoverImage(eq(COURSE_ID), any()))
                .willReturn(new CourseCoverUploadResponse(
                        COURSE_ID,
                        "https://res.cloudinary.com/demo/image/upload/edulife/course-covers/test.jpg",
                        "edulife/course-covers/test",
                        "Course cover image updated successfully"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "cover.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/v1/cms/courses/{id}/cover-image", COURSE_ID)
                        .file(file)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverImageUrl").value(
                        "https://res.cloudinary.com/demo/image/upload/edulife/course-covers/test.jpg"))
                .andExpect(jsonPath("$.coverImagePublicId").value("edulife/course-covers/test"))
                .andExpect(jsonPath("$.courseId").exists());
    }

    @Test
    void teacherNonOwnerReturns403() throws Exception {
        mockValidFirebaseToken();
        mockUserRole(UserRole.TEACHER);

        given(cmsCourseService.uploadCoverImage(eq(COURSE_ID), any()))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You do not have permission to update this course's cover image"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "cover.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/v1/cms/courses/{id}/cover-image", COURSE_ID)
                        .file(file)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void learnerReturns403() throws Exception {
        mockValidFirebaseToken();
        mockUserRole(UserRole.LEARNER);

        MockMultipartFile file = new MockMultipartFile(
                "file", "cover.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/v1/cms/courses/{id}/cover-image", COURSE_ID)
                        .file(file)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidFileTypeReturns400() throws Exception {
        mockValidFirebaseToken();
        mockUserRole(UserRole.TEACHER);

        given(cmsCourseService.uploadCoverImage(eq(COURSE_ID), any()))
                .willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Please upload a JPG, PNG, or WebP image"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", MediaType.APPLICATION_PDF_VALUE, new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/v1/cms/courses/{id}/cover-image", COURSE_ID)
                        .file(file)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void oversizedFileReturns413() throws Exception {
        mockValidFirebaseToken();
        mockUserRole(UserRole.TEACHER);

        given(cmsCourseService.uploadCoverImage(eq(COURSE_ID), any()))
                .willThrow(new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                        "Image must be smaller than 5MB"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "big.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[]{1});

        mockMvc.perform(multipart("/api/v1/cms/courses/{id}/cover-image", COURSE_ID)
                        .file(file)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void groupAdminUploadReturnsOk() throws Exception {
        mockValidFirebaseToken();
        mockUserRole(UserRole.GROUP_ADMIN);

        given(cmsCourseService.uploadCoverImage(eq(COURSE_ID), any()))
                .willReturn(new CourseCoverUploadResponse(
                        COURSE_ID,
                        "https://res.cloudinary.com/demo/image/upload/edulife/course-covers/ga.jpg",
                        "edulife/course-covers/ga",
                        "Course cover image updated successfully"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "cover.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/v1/cms/courses/{id}/cover-image", COURSE_ID)
                        .file(file)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverImageUrl").isString())
                .andExpect(jsonPath("$.coverImagePublicId").isString());
    }

    private void mockValidFirebaseToken() throws Exception {
        FirebaseToken decodedToken = org.mockito.Mockito.mock(FirebaseToken.class);
        given(firebaseAuth.verifyIdToken("valid-token")).willReturn(decodedToken);
        given(decodedToken.getUid()).willReturn("firebase-uid-456");
        given(decodedToken.getEmail()).willReturn("teacher@edulife.test");
        given(decodedToken.isEmailVerified()).willReturn(true);
    }

    private void mockUserRole(UserRole role) {
        User user = org.mockito.Mockito.mock(User.class);
        given(user.getId()).willReturn(USER_ID);
        given(user.getRole()).willReturn(role);
        given(userRepository.findByFirebaseUid("firebase-uid-456")).willReturn(Optional.of(user));
    }
}

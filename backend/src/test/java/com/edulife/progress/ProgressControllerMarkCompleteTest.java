package com.edulife.progress;

import com.edulife.common.error.ApiErrorWriter;
import com.edulife.common.error.GlobalApiExceptionHandler;
import com.edulife.progress.controller.ProgressController;
import com.edulife.progress.dto.CourseProgressDto;
import com.edulife.progress.service.ProgressService;
import com.edulife.security.SecurityConfig;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProgressController.class)
@Import({SecurityConfig.class, ApiErrorWriter.class, GlobalApiExceptionHandler.class})
class ProgressControllerMarkCompleteTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProgressService progressService;

    @MockBean
    private FirebaseAuth firebaseAuth;

    private static final UUID COURSE_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-aaaaaaaaaaaa");
    private static final UUID LESSON_ID = UUID.fromString("11111111-0000-0000-0000-111111111111");

    @Test
    void rejectsMarkCompleteWithNoToken() throws Exception {
        mockMvc.perform(post("/api/v1/courses/{courseId}/lessons/{lessonId}/complete",
                        COURSE_ID, LESSON_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(progressService);
    }

    @Test
    void returnsForbiddenWhenNotEnrolledToMarkComplete() throws Exception {
        mockValidFirebaseToken();
        willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Enroll in this course to mark lessons complete"))
                .given(progressService).markLessonComplete(COURSE_ID, LESSON_ID);

        mockMvc.perform(post("/api/v1/courses/{courseId}/lessons/{lessonId}/complete",
                        COURSE_ID, LESSON_ID)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Enroll in this course to mark lessons complete"));
    }

    @Test
    void returns204WhenMarkingLessonComplete() throws Exception {
        mockValidFirebaseToken();
        willDoNothing().given(progressService).markLessonComplete(COURSE_ID, LESSON_ID);

        mockMvc.perform(post("/api/v1/courses/{courseId}/lessons/{lessonId}/complete",
                        COURSE_ID, LESSON_ID)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNoContent());
    }

    @Test
    void returns204OnBothCallsConfirmingIdempotency() throws Exception {
        mockValidFirebaseToken();
        // Service is void on both calls — idempotency guard lives inside ProgressService
        willDoNothing().given(progressService).markLessonComplete(COURSE_ID, LESSON_ID);

        mockMvc.perform(post("/api/v1/courses/{courseId}/lessons/{lessonId}/complete",
                        COURSE_ID, LESSON_ID)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/courses/{courseId}/lessons/{lessonId}/complete",
                        COURSE_ID, LESSON_ID)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNoContent());
    }

    private void mockValidFirebaseToken() throws Exception {
        FirebaseToken decodedToken = mock(FirebaseToken.class);
        given(firebaseAuth.verifyIdToken("valid-token")).willReturn(decodedToken);
        given(decodedToken.getUid()).willReturn("firebase-uid-123");
        given(decodedToken.getEmail()).willReturn("student@edulife.test");
        given(decodedToken.isEmailVerified()).willReturn(true);
    }
}

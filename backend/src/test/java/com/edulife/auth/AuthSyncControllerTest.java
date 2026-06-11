package com.edulife.auth;

import com.edulife.users.repository.UserRepository;
import com.google.firebase.ErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private FirebaseAuth firebaseAuth;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void syncCreatesUserWhenFirebaseTokenIsValid() throws Exception {
        FirebaseToken decodedToken = org.mockito.Mockito.mock(FirebaseToken.class);

        given(firebaseAuth.verifyIdToken("valid-token")).willReturn(decodedToken);
        given(decodedToken.getUid()).willReturn("firebase-uid-123");
        given(decodedToken.getEmail()).willReturn("student@edulife.test");
        given(decodedToken.isEmailVerified()).willReturn(true);

        mockMvc.perform(post("/api/v1/auth/sync")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", notNullValue()))
                .andExpect(jsonPath("$.role").value("LEARNER"))
                .andExpect(jsonPath("$.firebaseUid").doesNotExist());

        assert userRepository.findByFirebaseUid("firebase-uid-123").isPresent();
    }

    @Test
    void syncReusesExistingUserOnRepeatLogin() throws Exception {
        FirebaseToken decodedToken = org.mockito.Mockito.mock(FirebaseToken.class);

        given(firebaseAuth.verifyIdToken("valid-token")).willReturn(decodedToken);
        given(decodedToken.getUid()).willReturn("firebase-uid-repeat");
        given(decodedToken.getEmail()).willReturn("repeat@edulife.test");
        given(decodedToken.isEmailVerified()).willReturn(true);

        String firstResponse = mockMvc.perform(post("/api/v1/auth/sync")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondResponse = mockMvc.perform(post("/api/v1/auth/sync")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assert firstResponse.equals(secondResponse);
        assert userRepository.findAll().size() == 1;
    }

    @Test
    void syncHonorsTeacherIntentOnlyOnFirstLogin() throws Exception {
        FirebaseToken decodedToken = org.mockito.Mockito.mock(FirebaseToken.class);

        given(firebaseAuth.verifyIdToken("teacher-token")).willReturn(decodedToken);
        given(decodedToken.getUid()).willReturn("firebase-uid-teacher");
        given(decodedToken.getEmail()).willReturn("teacher@edulife.test");
        given(decodedToken.isEmailVerified()).willReturn(true);

        mockMvc.perform(post("/api/v1/auth/sync")
                        .header("Authorization", "Bearer teacher-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"intendedRole\":\"TEACHER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TEACHER"));

        // Role intent is registration-only; later syncs must not demote or mutate trusted DB role.
        mockMvc.perform(post("/api/v1/auth/sync")
                        .header("Authorization", "Bearer teacher-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TEACHER"));
    }

    @Test
    void syncPreventsAdminSelfAssignment() throws Exception {
        FirebaseToken decodedToken = org.mockito.Mockito.mock(FirebaseToken.class);

        given(firebaseAuth.verifyIdToken("admin-intent-token")).willReturn(decodedToken);
        given(decodedToken.getUid()).willReturn("firebase-uid-admin-intent");
        given(decodedToken.getEmail()).willReturn("admin-intent@edulife.test");
        given(decodedToken.isEmailVerified()).willReturn(true);

        mockMvc.perform(post("/api/v1/auth/sync")
                        .header("Authorization", "Bearer admin-intent-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"intendedRole\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("LEARNER"));
    }

    @Test
    void syncRejectsInvalidOrExpiredToken() throws Exception {
        given(firebaseAuth.verifyIdToken("expired-token")).willThrow(
                new FirebaseAuthException(
                        ErrorCode.UNAUTHENTICATED,
                        "Token verification failed",
                        null,
                        null,
                        null
                )
        );

        mockMvc.perform(post("/api/v1/auth/sync")
                        .header("Authorization", "Bearer expired-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid or expired Firebase token"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void syncRejectsUnverifiedEmail() throws Exception {
        FirebaseToken decodedToken = org.mockito.Mockito.mock(FirebaseToken.class);

        given(firebaseAuth.verifyIdToken("unverified-token")).willReturn(decodedToken);
        given(decodedToken.getUid()).willReturn("firebase-uid-unverified");
        given(decodedToken.getEmail()).willReturn("unverified@edulife.test");
        given(decodedToken.isEmailVerified()).willReturn(false);

        mockMvc.perform(post("/api/v1/auth/sync")
                        .header("Authorization", "Bearer unverified-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Email is not verified"))
                .andExpect(jsonPath("$.timestamp").exists());

        assert userRepository.findByFirebaseUid("firebase-uid-unverified").isEmpty();
    }

    @Test
    void syncRejectsMissingToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/sync"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}

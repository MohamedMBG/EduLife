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
                .andExpect(status().isUnauthorized());
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
                .andExpect(status().isForbidden());

        assert userRepository.findByFirebaseUid("firebase-uid-unverified").isEmpty();
    }

    @Test
    void syncRejectsMissingToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/sync"))
                .andExpect(status().isUnauthorized());
    }
}
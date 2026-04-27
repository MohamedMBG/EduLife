package com.edulife.security;

import com.google.firebase.ErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TestSecurityController.class)
@Import(SecurityConfig.class)
class FirebaseTokenFilterSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FirebaseAuth firebaseAuth;

    @Test
    void authenticatesProtectedEndpointWhenFirebaseTokenIsValid() throws Exception {
        FirebaseToken decodedToken = org.mockito.Mockito.mock(FirebaseToken.class);
        given(firebaseAuth.verifyIdToken("valid-token")).willReturn(decodedToken);
        given(decodedToken.getUid()).willReturn("firebase-uid-123");
        given(decodedToken.getEmail()).willReturn("student@edulife.test");
        given(decodedToken.isEmailVerified()).willReturn(true);

        mockMvc.perform(get("/api/v1/secure/profile")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firebaseUid").value("firebase-uid-123"))
                .andExpect(jsonPath("$.email").value("student@edulife.test"));
    }

    @Test
    void rejectsMissingTokenOnProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/secure/profile"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(firebaseAuth);
    }

    @Test
    void rejectsMalformedAuthorizationHeaderOnProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/secure/profile")
                        .header("Authorization", "Token invalid-format"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(firebaseAuth);
    }

    @Test
    void rejectsInvalidOrExpiredFirebaseToken() throws Exception {
        given(firebaseAuth.verifyIdToken("expired-token")).willThrow(
                new FirebaseAuthException(ErrorCode.UNAUTHENTICATED, "Token verification failed", null, null, null)
        );

        mockMvc.perform(get("/api/v1/secure/profile")
                        .header("Authorization", "Bearer expired-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsUnverifiedEmailOnProtectedEndpoint() throws Exception {
        FirebaseToken decodedToken = org.mockito.Mockito.mock(FirebaseToken.class);
        given(firebaseAuth.verifyIdToken("unverified-token")).willReturn(decodedToken);
        given(decodedToken.isEmailVerified()).willReturn(false);

        mockMvc.perform(get("/api/v1/secure/profile")
                        .header("Authorization", "Bearer unverified-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void leavesPublicEndpointAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/public/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        verifyNoInteractions(firebaseAuth);
    }

    @Test
    void ignoresMalformedAuthorizationHeaderOnPublicEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/public/ping")
                        .header("Authorization", "Token invalid-format"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        verifyNoInteractions(firebaseAuth);
    }
}

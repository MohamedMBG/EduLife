package com.edulife.security;

import com.edulife.common.error.ApiErrorWriter;
import com.edulife.common.error.GlobalApiExceptionHandler;
import com.edulife.users.repository.UserRepository;
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
@Import({SecurityConfig.class, ApiErrorWriter.class, GlobalApiExceptionHandler.class})
class FirebaseTokenFilterSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FirebaseAuth firebaseAuth;

    @MockBean
    private UserRepository userRepository;

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
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(firebaseAuth);
    }

    @Test
    void rejectsMalformedAuthorizationHeaderOnProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/secure/profile")
                        .header("Authorization", "Token invalid-format"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Malformed Authorization header"))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(firebaseAuth);
    }

    @Test
    void rejectsInvalidOrExpiredFirebaseToken() throws Exception {
        given(firebaseAuth.verifyIdToken("expired-token")).willThrow(
                new FirebaseAuthException(ErrorCode.UNAUTHENTICATED, "Token verification failed", null, null, null)
        );

        mockMvc.perform(get("/api/v1/secure/profile")
                        .header("Authorization", "Bearer expired-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid or expired Firebase token"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void rejectsUnverifiedEmailOnProtectedEndpoint() throws Exception {
        FirebaseToken decodedToken = org.mockito.Mockito.mock(FirebaseToken.class);
        given(firebaseAuth.verifyIdToken("unverified-token")).willReturn(decodedToken);
        given(decodedToken.isEmailVerified()).willReturn(false);

        mockMvc.perform(get("/api/v1/secure/profile")
                        .header("Authorization", "Bearer unverified-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Email is not verified"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void rejectsFormerPublicApiEndpointWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/public/ping"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(firebaseAuth);
    }

    @Test
    void rejectsMalformedAuthorizationHeaderOnFormerPublicApiEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/public/ping")
                        .header("Authorization", "Token invalid-format"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Malformed Authorization header"))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(firebaseAuth);
    }

    @Test
    void formatsBadRequestExceptionsWithApiErrorContract() throws Exception {
        FirebaseToken decodedToken = org.mockito.Mockito.mock(FirebaseToken.class);
        given(firebaseAuth.verifyIdToken("valid-token")).willReturn(decodedToken);
        given(decodedToken.getUid()).willReturn("firebase-uid-123");
        given(decodedToken.getEmail()).willReturn("student@edulife.test");
        given(decodedToken.isEmailVerified()).willReturn(true);

        mockMvc.perform(get("/api/v1/secure/bad-request")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid test request"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void formatsUnexpectedExceptionsWithSafeApiErrorContract() throws Exception {
        FirebaseToken decodedToken = org.mockito.Mockito.mock(FirebaseToken.class);
        given(firebaseAuth.verifyIdToken("valid-token")).willReturn(decodedToken);
        given(decodedToken.getUid()).willReturn("firebase-uid-123");
        given(decodedToken.getEmail()).willReturn("student@edulife.test");
        given(decodedToken.isEmailVerified()).willReturn(true);

        mockMvc.perform(get("/api/v1/secure/server-error")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Internal server error"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}

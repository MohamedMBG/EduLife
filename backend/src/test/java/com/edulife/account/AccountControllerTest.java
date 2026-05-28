package com.edulife.account;

import com.edulife.account.controller.AccountController;
import com.edulife.account.service.AccountService;
import com.edulife.common.error.ApiErrorWriter;
import com.edulife.common.error.GlobalApiExceptionHandler;
import com.edulife.security.SecurityConfig;
import com.edulife.users.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@Import({SecurityConfig.class, ApiErrorWriter.class, GlobalApiExceptionHandler.class})
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @MockBean
    private FirebaseAuth firebaseAuth;

    @MockBean
    private UserRepository userRepository;

    @Test
    void deleteAccountReturns204OnSuccess() throws Exception {
        mockValidFirebaseToken();
        willDoNothing().given(accountService).deleteCurrentAccount();

        mockMvc.perform(delete("/api/v1/account")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNoContent());

        verify(accountService).deleteCurrentAccount();
    }

    @Test
    void deleteAccountReturns401WithoutToken() throws Exception {
        mockMvc.perform(delete("/api/v1/account"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));

        verifyNoInteractions(accountService);
    }

    @Test
    void deleteAccountReturns500WhenFirebaseDeleteFails() throws Exception {
        mockValidFirebaseToken();
        willThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not finalize account deletion"))
                .given(accountService).deleteCurrentAccount();

        mockMvc.perform(delete("/api/v1/account")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Could not finalize account deletion"));
    }

    @Test
    void deleteAccountReturns401WhenUserNotSynced() throws Exception {
        mockValidFirebaseToken();
        willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "User not found. Call /auth/sync first."))
                .given(accountService).deleteCurrentAccount();

        mockMvc.perform(delete("/api/v1/account")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("User not found. Call /auth/sync first."));
    }

    private void mockValidFirebaseToken() throws Exception {
        FirebaseToken decodedToken = org.mockito.Mockito.mock(FirebaseToken.class);
        given(firebaseAuth.verifyIdToken("valid-token")).willReturn(decodedToken);
        given(decodedToken.getUid()).willReturn("firebase-uid-123");
        given(decodedToken.getEmail()).willReturn("student@edulife.test");
        given(decodedToken.isEmailVerified()).willReturn(true);
    }
}

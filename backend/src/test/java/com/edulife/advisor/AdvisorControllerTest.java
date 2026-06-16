package com.edulife.advisor;

import com.edulife.advisor.controller.AdvisorController;
import com.edulife.advisor.dto.AdvisorResponse;
import com.edulife.advisor.service.AdvisorService;
import com.edulife.common.error.ApiErrorWriter;
import com.edulife.common.error.GlobalApiExceptionHandler;
import com.edulife.security.SecurityConfig;
import com.edulife.users.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdvisorController.class)
@Import({SecurityConfig.class, ApiErrorWriter.class, GlobalApiExceptionHandler.class})
class AdvisorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdvisorService advisorService;

    @MockBean
    private FirebaseAuth firebaseAuth;

    @MockBean
    private UserRepository userRepository;

    // ── POST /api/v1/advisor/recommend ───────────────────────────────────────

    @Test
    void rejectsRequestWithNoToken() throws Exception {
        mockMvc.perform(post("/api/v1/advisor/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goal\":\"I want to become a software developer\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(advisorService);
    }

    @Test
    void rejectsBlankGoal() throws Exception {
        mockValidFirebaseToken();

        mockMvc.perform(post("/api/v1/advisor/recommend")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goal\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(advisorService);
    }

    @Test
    void rejectsMissingGoalField() throws Exception {
        mockValidFirebaseToken();

        mockMvc.perform(post("/api/v1/advisor/recommend")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(advisorService);
    }

    @Test
    void rejectsGoalExceedingMaxLength() throws Exception {
        mockValidFirebaseToken();
        String tooLong = "a".repeat(501);

        mockMvc.perform(post("/api/v1/advisor/recommend")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goal\":\"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(advisorService);
    }

    @Test
    void returnsStubResponseForValidRequest() throws Exception {
        mockValidFirebaseToken();
        given(advisorService.recommend(any()))
                .willReturn(new AdvisorResponse("Advisor service not yet connected", List.of()));

        mockMvc.perform(post("/api/v1/advisor/recommend")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goal\":\"I want to become a software developer\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Advisor service not yet connected"))
                .andExpect(jsonPath("$.recommendations").isArray())
                .andExpect(jsonPath("$.recommendations").isEmpty());
    }

    private void mockValidFirebaseToken() throws Exception {
        FirebaseToken decodedToken = mock(FirebaseToken.class);
        given(firebaseAuth.verifyIdToken("valid-token")).willReturn(decodedToken);
        given(decodedToken.getUid()).willReturn("firebase-uid-123");
        given(decodedToken.getEmail()).willReturn("student@edulife.test");
        given(decodedToken.isEmailVerified()).willReturn(true);
    }
}

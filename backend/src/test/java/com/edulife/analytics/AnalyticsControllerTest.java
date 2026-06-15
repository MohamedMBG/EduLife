package com.edulife.analytics;

import com.edulife.analytics.controller.AnalyticsController;
import com.edulife.analytics.dto.PlatformAnalyticsDto;
import com.edulife.analytics.dto.StudentAnalyticsSummaryDto;
import com.edulife.analytics.dto.TeacherAnalyticsDto;
import com.edulife.analytics.service.AnalyticsService;
import com.edulife.common.error.ApiErrorWriter;
import com.edulife.common.error.GlobalApiExceptionHandler;
import com.edulife.security.SecurityConfig;
import com.edulife.users.entity.User;
import com.edulife.users.model.UserRole;
import com.edulife.users.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RBAC boundary tests for the analytics endpoints. Verifies token requirement and role gating;
 * ownership scoping itself is covered by AnalyticsServiceTest.
 */
@WebMvcTest(AnalyticsController.class)
@Import({SecurityConfig.class, ApiErrorWriter.class, GlobalApiExceptionHandler.class})
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private FirebaseAuth firebaseAuth;

    @MockBean
    private UserRepository userRepository;

    // ── no token ────────────────────────────────────────────────────────────────

    @Test
    void platformAnalytics_rejectsRequestWithNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/platform"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        verifyNoInteractions(analyticsService);
    }

    @Test
    void teacherAnalytics_rejectsRequestWithNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/teacher/courses"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(analyticsService);
    }

    @Test
    void studentSummary_rejectsRequestWithNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/me/summary"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(analyticsService);
    }

    // ── role gating ──────────────────────────────────────────────────────────────

    @Test
    void platformAnalytics_forbiddenForLearner() throws Exception {
        mockTokenForRole(UserRole.LEARNER);

        mockMvc.perform(get("/api/v1/analytics/platform").header("Authorization", "Bearer t"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(analyticsService);
    }

    @Test
    void platformAnalytics_forbiddenForTeacher() throws Exception {
        mockTokenForRole(UserRole.TEACHER);

        mockMvc.perform(get("/api/v1/analytics/platform").header("Authorization", "Bearer t"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(analyticsService);
    }

    @Test
    void teacherAnalytics_forbiddenForLearner() throws Exception {
        mockTokenForRole(UserRole.LEARNER);

        mockMvc.perform(get("/api/v1/analytics/teacher/courses").header("Authorization", "Bearer t"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(analyticsService);
    }

    // ── allowed paths ──────────────────────────────────────────────────────────────

    @Test
    void platformAnalytics_allowedForAdmin() throws Exception {
        mockTokenForRole(UserRole.ADMIN);
        given(analyticsService.getPlatformAnalytics())
                .willReturn(new PlatformAnalyticsDto(5, 2, 1, 1, 0, 3, 0, 4, 6, 2, 1));

        mockMvc.perform(get("/api/v1/analytics/platform").header("Authorization", "Bearer t"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.learners").value(5))
                .andExpect(jsonPath("$.coursesPublished").value(3));
    }

    @Test
    void teacherAnalytics_allowedForTeacher() throws Exception {
        mockTokenForRole(UserRole.TEACHER);
        given(analyticsService.getMyTeacherAnalytics())
                .willReturn(new TeacherAnalyticsDto(0, List.of()));

        mockMvc.perform(get("/api/v1/analytics/teacher/courses").header("Authorization", "Bearer t"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCourses").value(0));
    }

    @Test
    void studentSummary_allowedForAnyAuthenticatedLearner() throws Exception {
        mockTokenForRole(UserRole.LEARNER);
        given(analyticsService.getMyStudentSummary())
                .willReturn(new StudentAnalyticsSummaryDto(2, 7, 3, 1, 1));

        mockMvc.perform(get("/api/v1/analytics/me/summary").header("Authorization", "Bearer t"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeEnrollments").value(2))
                .andExpect(jsonPath("$.certificatesEarned").value(1));
    }

    /**
     * Mocks a valid, email-verified Firebase token and a synced user with the given role so
     * FirebaseTokenFilter assigns ROLE_&lt;role&gt; authorities. Role is sourced from the trusted
     * users table, never from the client.
     */
    private void mockTokenForRole(UserRole role) throws Exception {
        FirebaseToken token = mock(FirebaseToken.class);
        given(firebaseAuth.verifyIdToken("t")).willReturn(token);
        given(token.getUid()).willReturn("uid-" + role.name());
        given(token.getEmail()).willReturn("user@test.com");
        given(token.isEmailVerified()).willReturn(true);

        User user = mock(User.class);
        given(user.getRole()).willReturn(role);
        given(userRepository.findByFirebaseUid("uid-" + role.name())).willReturn(Optional.of(user));
    }
}

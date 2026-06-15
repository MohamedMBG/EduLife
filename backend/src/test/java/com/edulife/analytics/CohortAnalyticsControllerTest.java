package com.edulife.analytics;

import com.edulife.analytics.controller.CohortAnalyticsController;
import com.edulife.analytics.dto.FunnelDto;
import com.edulife.analytics.dto.GroupCohortAnalyticsDto;
import com.edulife.analytics.dto.PlatformCohortAnalyticsDto;
import com.edulife.analytics.dto.StudentProgressTrendDto;
import com.edulife.analytics.dto.TeacherCohortAnalyticsDto;
import com.edulife.analytics.service.CohortAnalyticsService;
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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RBAC boundary tests for every Phase C cohort endpoint. Scoping is covered by the service test. */
@WebMvcTest(CohortAnalyticsController.class)
@Import({SecurityConfig.class, ApiErrorWriter.class, GlobalApiExceptionHandler.class})
class CohortAnalyticsControllerTest {

    private static final String GROUP_PATH =
            "/api/v1/analytics/group/44444444-4444-4444-4444-444444444444/cohorts";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CohortAnalyticsService cohortService;

    @MockBean
    private FirebaseAuth firebaseAuth;

    @MockBean
    private UserRepository userRepository;

    // ── no token ────────────────────────────────────────────────────────────────

    @Test
    void progressTrend_rejectsNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/me/progress-trend"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(cohortService);
    }

    @Test
    void teacherCohorts_rejectsNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/teacher/cohorts"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(cohortService);
    }

    @Test
    void groupCohorts_rejectsNoToken() throws Exception {
        mockMvc.perform(get(GROUP_PATH)).andExpect(status().isUnauthorized());
        verifyNoInteractions(cohortService);
    }

    @Test
    void platformCohorts_rejectsNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/platform/cohorts"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(cohortService);
    }

    // ── role gating ──────────────────────────────────────────────────────────────

    @Test
    void teacherCohorts_forbiddenForLearner() throws Exception {
        mockTokenForRole(UserRole.LEARNER);
        mockMvc.perform(get("/api/v1/analytics/teacher/cohorts").header("Authorization", "Bearer t"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(cohortService);
    }

    @Test
    void groupCohorts_forbiddenForLearner() throws Exception {
        mockTokenForRole(UserRole.LEARNER);
        mockMvc.perform(get(GROUP_PATH).header("Authorization", "Bearer t"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(cohortService);
    }

    @Test
    void groupCohorts_forbiddenForTeacher() throws Exception {
        // Group analytics is GROUP_ADMIN/ADMIN only; a teacher must not reach it.
        mockTokenForRole(UserRole.TEACHER);
        mockMvc.perform(get(GROUP_PATH).header("Authorization", "Bearer t"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(cohortService);
    }

    @Test
    void platformCohorts_forbiddenForGroupAdmin() throws Exception {
        mockTokenForRole(UserRole.GROUP_ADMIN);
        mockMvc.perform(get("/api/v1/analytics/platform/cohorts").header("Authorization", "Bearer t"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(cohortService);
    }

    // ── allowed paths ──────────────────────────────────────────────────────────────

    @Test
    void progressTrend_allowedForLearner() throws Exception {
        mockTokenForRole(UserRole.LEARNER);
        given(cohortService.getMyProgressTrend())
                .willReturn(new StudentProgressTrendDto(7, List.of()));
        mockMvc.perform(get("/api/v1/analytics/me/progress-trend").header("Authorization", "Bearer t"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLessons").value(7));
    }

    @Test
    void teacherCohorts_allowedForTeacher() throws Exception {
        mockTokenForRole(UserRole.TEACHER);
        given(cohortService.getMyTeacherCohorts())
                .willReturn(new TeacherCohortAnalyticsDto(2, FunnelDto.empty(), List.of()));
        mockMvc.perform(get("/api/v1/analytics/teacher/cohorts").header("Authorization", "Bearer t"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseCount").value(2));
    }

    @Test
    void groupCohorts_allowedForGroupAdmin() throws Exception {
        mockTokenForRole(UserRole.GROUP_ADMIN);
        given(cohortService.getGroupCohorts(any(UUID.class)))
                .willReturn(new GroupCohortAnalyticsDto(
                        UUID.fromString("44444444-4444-4444-4444-444444444444"),
                        "Cohort A", 3, 2, FunnelDto.empty()));
        mockMvc.perform(get(GROUP_PATH).header("Authorization", "Bearer t"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupName").value("Cohort A"));
    }

    @Test
    void platformCohorts_allowedForAdmin() throws Exception {
        mockTokenForRole(UserRole.ADMIN);
        given(cohortService.getPlatformCohorts())
                .willReturn(new PlatformCohortAnalyticsDto(FunnelDto.empty(), List.of(), List.of()));
        mockMvc.perform(get("/api/v1/analytics/platform/cohorts").header("Authorization", "Bearer t"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.funnel.enrolled").value(0));
    }

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

package com.edulife.gamification;

import com.edulife.common.error.ApiErrorWriter;
import com.edulife.common.error.GlobalApiExceptionHandler;
import com.edulife.gamification.controller.GamificationController;
import com.edulife.gamification.dto.BadgeDto;
import com.edulife.gamification.dto.GamificationStateDto;
import com.edulife.gamification.dto.LeaderboardEntryDto;
import com.edulife.gamification.service.GamificationService;
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

/**
 * Verifies that gamification endpoints sit behind Firebase auth and never accept
 * a client-supplied userId — every read scopes off the resolved Firebase identity.
 */
@WebMvcTest(GamificationController.class)
@Import({SecurityConfig.class, ApiErrorWriter.class, GlobalApiExceptionHandler.class})
class GamificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GamificationService gamificationService;

    @MockBean
    private FirebaseAuth firebaseAuth;

    @MockBean
    private UserRepository userRepository;

    @Test
    void getMyStateRequiresFirebaseToken() throws Exception {
        mockMvc.perform(get("/api/v1/gamification/me"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(gamificationService);
    }

    @Test
    void getLeaderboardRequiresFirebaseToken() throws Exception {
        mockMvc.perform(get("/api/v1/gamification/leaderboard"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(gamificationService);
    }

    @Test
    void getMyStateReturnsResolvedUserState() throws Exception {
        UUID userId = mockAuthenticatedLearner();

        GamificationStateDto dto = new GamificationStateDto(
                350, 2, "Curious", 250, 600, 100, 350, 4, 4, null, List.of());
        given(gamificationService.getState(userId)).willReturn(dto);

        mockMvc.perform(get("/api/v1/gamification/me").header("Authorization", "Bearer t"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalXp").value(350))
                .andExpect(jsonPath("$.level").value(2))
                .andExpect(jsonPath("$.levelName").value("Curious"));
    }

    @Test
    void getLeaderboardReturnsRankedEntries() throws Exception {
        mockAuthenticatedLearner();

        given(gamificationService.getLeaderboard(20)).willReturn(List.of(
                new LeaderboardEntryDto(1, UUID.randomUUID(), "Top", 999, 5, "Thinker"),
                new LeaderboardEntryDto(2, UUID.randomUUID(), "Second", 500, 3, "Explorer")
        ));

        mockMvc.perform(get("/api/v1/gamification/leaderboard").header("Authorization", "Bearer t"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[0].displayName").value("Top"))
                .andExpect(jsonPath("$[1].totalXp").value(500));
    }

    @Test
    void getBadgeCatalogReturnsAllDefinitions() throws Exception {
        mockAuthenticatedLearner();
        given(gamificationService.listBadgeDefinitions()).willReturn(List.of(
                new BadgeDto("first_flame", "First Flame", "COMMON", "Complete your first lesson.", false, null)
        ));

        mockMvc.perform(get("/api/v1/gamification/badges").header("Authorization", "Bearer t"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("first_flame"))
                .andExpect(jsonPath("$[0].rarity").value("COMMON"));
    }

    private UUID mockAuthenticatedLearner() throws Exception {
        FirebaseToken token = mock(FirebaseToken.class);
        given(firebaseAuth.verifyIdToken("t")).willReturn(token);
        given(token.getUid()).willReturn("uid-learner");
        given(token.getEmail()).willReturn("learner@test.com");
        given(token.isEmailVerified()).willReturn(true);

        UUID userId = UUID.fromString("12345678-1234-1234-1234-123456789012");
        User user = mock(User.class);
        given(user.getId()).willReturn(userId);
        given(user.getRole()).willReturn(UserRole.LEARNER);
        given(userRepository.findByFirebaseUid("uid-learner")).willReturn(Optional.of(user));
        return userId;
    }
}

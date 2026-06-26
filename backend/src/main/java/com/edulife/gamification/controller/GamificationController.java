package com.edulife.gamification.controller;

import com.edulife.gamification.dto.BadgeDto;
import com.edulife.gamification.dto.GamificationStateDto;
import com.edulife.gamification.dto.LeaderboardEntryDto;
import com.edulife.gamification.service.GamificationService;
import com.edulife.security.FirebaseAuthentication;
import com.edulife.users.entity.User;
import com.edulife.users.repository.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Read-only gamification endpoints. PR A exposes only what the clients need to
 * stop computing XP / levels / streaks / badges locally. Writes are emitted by
 * the relevant domain services; nothing here accepts a userId from the client.
 */
@RestController
@RequestMapping("/api/v1/gamification")
public class GamificationController {

    private final GamificationService gamificationService;
    private final UserRepository userRepository;

    public GamificationController(
            GamificationService gamificationService,
            UserRepository userRepository
    ) {
        this.gamificationService = gamificationService;
        this.userRepository = userRepository;
    }

    /** Returns the authenticated learner's XP, level, streak, and badge state. */
    @GetMapping("/me")
    public GamificationStateDto getMyState() {
        User user = resolveCurrentUser();
        return gamificationService.getState(user.getId());
    }

    /** Returns the global all-time leaderboard, capped at the requested limit (default 20, max 100). */
    @GetMapping("/leaderboard")
    public List<LeaderboardEntryDto> getLeaderboard(
            @RequestParam(value = "limit", required = false, defaultValue = "20") int limit
    ) {
        return gamificationService.getLeaderboard(limit);
    }

    /** Returns all 12 badge definitions from the catalog (without user-specific unlock status). */
    @GetMapping("/badges")
    public List<BadgeDto> getBadgeCatalog() {
        return gamificationService.listBadgeDefinitions();
    }

    /** Resolves the internal user from the Firebase-authenticated security context. */
    private User resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof FirebaseAuthentication firebaseAuth)) {
            throw new IllegalStateException("Firebase authentication required");
        }
        return userRepository.findByFirebaseUid(firebaseAuth.getFirebaseUid())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User not found. Call /auth/sync first."));
    }
}

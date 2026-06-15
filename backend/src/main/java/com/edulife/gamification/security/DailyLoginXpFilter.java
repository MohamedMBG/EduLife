package com.edulife.gamification.security;

import com.edulife.gamification.service.GamificationService;
import com.edulife.security.FirebaseAuthentication;
import com.edulife.users.entity.User;
import com.edulife.users.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Awards the daily-login XP exactly once per user per UTC day. A short-lived
 * in-memory cache avoids hitting the DB on every authenticated request after
 * the day has already been claimed; the database dedup_key is still the source
 * of truth for cross-instance correctness.
 */
public class DailyLoginXpFilter extends OncePerRequestFilter {

    private static final ZoneId ACTIVITY_ZONE = ZoneId.of("UTC");

    private final UserRepository userRepository;
    private final GamificationService gamificationService;
    private final Clock clock;
    private final ConcurrentMap<UUID, LocalDate> awardedToday = new ConcurrentHashMap<>();

    public DailyLoginXpFilter(
            UserRepository userRepository,
            GamificationService gamificationService,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.gamificationService = gamificationService;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip public assets and health probes; they may not be Firebase-authenticated.
        return path.startsWith("/actuator")
                || path.startsWith("/uploads/")
                || path.startsWith("/api/v1/certificates/verify/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            awardIfDue();
        } catch (RuntimeException ignored) {
            // Gamification must never block the request pipeline. A failed login award
            // can be retried tomorrow without affecting the user's primary action.
        }
        filterChain.doFilter(request, response);
    }

    private void awardIfDue() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof FirebaseAuthentication firebaseAuth)) {
            return;
        }
        String firebaseUid = firebaseAuth.getFirebaseUid();
        if (firebaseUid == null) {
            return;
        }

        User user = userRepository.findByFirebaseUid(firebaseUid).orElse(null);
        if (user == null) {
            // Unsynced firebase identity — /auth/sync has not run yet, so there is no
            // backend user row to award against.
            return;
        }

        LocalDate today = LocalDate.now(clock.withZone(ACTIVITY_ZONE));
        LocalDate cached = awardedToday.get(user.getId());
        if (today.equals(cached)) {
            return;
        }

        gamificationService.onDailyLogin(user.getId());
        awardedToday.put(user.getId(), today);
    }
}

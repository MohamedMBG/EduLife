package com.baghdad.edulife.features.gamification.data;

import androidx.annotation.NonNull;

import com.baghdad.edulife.R;
import com.baghdad.edulife.core.network.ApiClient;
import com.baghdad.edulife.core.network.ApiService;
import com.baghdad.edulife.features.gamification.model.Badge;
import com.baghdad.edulife.features.gamification.model.BadgeRarity;
import com.baghdad.edulife.features.gamification.model.BadgeResponse;
import com.baghdad.edulife.features.gamification.model.GamificationStateResponse;
import com.baghdad.edulife.features.gamification.model.GamificationUiState;
import com.baghdad.edulife.features.gamification.model.LeaderboardEntryResponse;
import com.baghdad.edulife.features.gamification.model.LevelInfo;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Backend-fetched gamification state. The Android client renders whatever the
 * backend reports and never computes XP, level, streak, or badge unlocks
 * locally (per CLAUDE.md gamification spec).
 *
 * Auth follows the same path as every other repository here: the Firebase
 * Bearer token is added by FirebaseAuthInterceptor and a 401 retry happens at
 * the OkHttp authenticator layer, so callbacks only handle business errors.
 */
public class GamificationRepository {

    public interface StateCallback {
        void onSuccess(GamificationUiState state);
        void onError(String message);
    }

    public interface LeaderboardCallback {
        void onSuccess(List<LeaderboardEntryResponse> entries);
        void onError(String message);
    }

    private final ApiService apiService;

    public GamificationRepository() {
        this.apiService = ApiClient.getClient().create(ApiService.class);
    }

    public void loadMyState(StateCallback callback) {
        apiService.getMyGamificationState().enqueue(new Callback<GamificationStateResponse>() {
            @Override
            public void onResponse(@NonNull Call<GamificationStateResponse> call,
                                   @NonNull Response<GamificationStateResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Failed to load gamification state. Status: " + response.code());
                    return;
                }
                callback.onSuccess(toUiState(response.body()));
            }

            @Override
            public void onFailure(@NonNull Call<GamificationStateResponse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    public void loadLeaderboard(int limit, LeaderboardCallback callback) {
        apiService.getGamificationLeaderboard(limit).enqueue(new Callback<List<LeaderboardEntryResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<LeaderboardEntryResponse>> call,
                                   @NonNull Response<List<LeaderboardEntryResponse>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Failed to load leaderboard. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<List<LeaderboardEntryResponse>> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    // ── Wire → UI mapping ──────────────────────────────────────────────────

    private static GamificationUiState toUiState(GamificationStateResponse body) {
        // The backend reports xpForNextLevel == 0 at max level. The Android UI
        // expects Integer.MAX_VALUE in that case so the "to next level" copy
        // collapses to the max-level string.
        int xpForNext = body.nextLevelXp <= body.currentLevelXp
                ? Integer.MAX_VALUE
                : body.nextLevelXp;

        int progressPercent;
        if (xpForNext == Integer.MAX_VALUE || body.xpForNextLevel <= 0) {
            progressPercent = 100;
        } else {
            progressPercent = Math.max(0, Math.min(100,
                    (body.xpIntoLevel * 100) / body.xpForNextLevel));
        }

        LevelInfo levelInfo = new LevelInfo(
                body.level,
                body.levelName,
                body.totalXp,
                body.currentLevelXp,
                xpForNext,
                progressPercent
        );

        List<Badge> badges = new ArrayList<>();
        if (body.badges != null) {
            for (BadgeResponse br : body.badges) {
                badges.add(toBadge(br));
            }
        }

        int unlocked = 0;
        for (Badge b : badges) {
            if (b.earned) unlocked++;
        }

        // lessonsCompleted / coursesEnrolled / certificatesEarned aren't part of
        // the gamification payload — they will move with the analytics screen if
        // ever needed; pass derived placeholders so the existing layout still
        // binds without exposing stale local counters.
        return new GamificationUiState(
                body.totalXp,
                levelInfo,
                body.currentStreak,
                badges,
                /* lessonsCompleted */ 0,
                /* coursesEnrolled */ 0,
                /* certificatesEarned */ unlocked,
                body.longestStreak,
                body.lastActivityDate
        );
    }

    private static Badge toBadge(BadgeResponse br) {
        return new Badge(
                br.id,
                br.label,
                br.unlockDescription == null ? "" : br.unlockDescription,
                iconFor(br.id),
                emojiFor(br.id),
                rarityOf(br.rarity),
                br.unlocked
        );
    }

    private static BadgeRarity rarityOf(String name) {
        if (name == null) return BadgeRarity.COMMON;
        try {
            return BadgeRarity.valueOf(name);
        } catch (IllegalArgumentException e) {
            return BadgeRarity.COMMON;
        }
    }

    private static int iconFor(String id) {
        if (id == null) return R.drawable.ic_xp_star;
        switch (id) {
            case "first_flame":   return R.drawable.ic_badge_first_steps;
            case "bookworm":      return R.drawable.ic_badge_bookworm;
            case "speed_run":     return R.drawable.ic_badge_on_fire;
            case "sharp_mind":    return R.drawable.ic_badge_champion;
            case "graduate":      return R.drawable.ic_badge_certified;
            case "on_a_roll":     return R.drawable.ic_badge_polymath;
            case "dedicated":     return R.drawable.ic_badge_dedicated;
            case "star_learner":  return R.drawable.ic_badge_unstoppable;
            case "scholar":       return R.drawable.ic_badge_bookworm;
            case "master":        return R.drawable.ic_badge_champion;
            case "trophy_hunter": return R.drawable.ic_badge_champion;
            case "inferno":       return R.drawable.ic_badge_on_fire;
            default:              return R.drawable.ic_xp_star;
        }
    }

    private static String emojiFor(String id) {
        if (id == null) return "?";
        switch (id) {
            case "first_flame":   return "🔥";
            case "bookworm":      return "📚";
            case "speed_run":     return "⚡";
            case "sharp_mind":    return "🎯";
            case "graduate":      return "🎓";
            case "on_a_roll":     return "📈";
            case "dedicated":     return "🛡️";
            case "star_learner":  return "⭐";
            case "scholar":       return "📜";
            case "master":        return "👑";
            case "trophy_hunter": return "🏆";
            case "inferno":       return "🔥";
            default:              return "?";
        }
    }
}

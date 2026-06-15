package com.baghdad.edulife.features.gamification.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Raw response shape for GET /api/v1/gamification/me. Mapped to {@link GamificationUiState}
 * by the repository so the rest of the app stays decoupled from wire types.
 */
public class GamificationStateResponse {

    @SerializedName("totalXp")
    public int totalXp;

    @SerializedName("level")
    public int level;

    @SerializedName("levelName")
    public String levelName;

    @SerializedName("currentLevelXp")
    public int currentLevelXp;

    @SerializedName("nextLevelXp")
    public int nextLevelXp;

    @SerializedName("xpIntoLevel")
    public int xpIntoLevel;

    @SerializedName("xpForNextLevel")
    public int xpForNextLevel;

    @SerializedName("currentStreak")
    public int currentStreak;

    @SerializedName("longestStreak")
    public int longestStreak;

    @SerializedName("lastActivityDate")
    public String lastActivityDate;

    @SerializedName("badges")
    public List<BadgeResponse> badges;
}

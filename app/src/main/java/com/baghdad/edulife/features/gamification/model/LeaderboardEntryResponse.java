package com.baghdad.edulife.features.gamification.model;

import com.google.gson.annotations.SerializedName;

public class LeaderboardEntryResponse {

    @SerializedName("rank")
    public int rank;

    @SerializedName("userId")
    public String userId;

    @SerializedName("displayName")
    public String displayName;

    @SerializedName("totalXp")
    public int totalXp;

    @SerializedName("level")
    public int level;

    @SerializedName("levelName")
    public String levelName;
}

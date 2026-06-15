package com.baghdad.edulife.features.gamification.model;

import com.google.gson.annotations.SerializedName;

/**
 * Wire-level representation of a badge returned by /api/v1/gamification.
 * The Android UI builds {@link Badge} from this so icons + rarity colors stay
 * platform-specific while ids/labels/unlock state come from the backend.
 */
public class BadgeResponse {

    @SerializedName("id")
    public String id;

    @SerializedName("label")
    public String label;

    @SerializedName("rarity")
    public String rarity;

    @SerializedName("unlockDescription")
    public String unlockDescription;

    @SerializedName("unlocked")
    public boolean unlocked;

    @SerializedName("unlockedAt")
    public String unlockedAt;
}

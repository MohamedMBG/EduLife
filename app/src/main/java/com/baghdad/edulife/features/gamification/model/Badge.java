package com.baghdad.edulife.features.gamification.model;

public class Badge {

    public final String id;
    public final String name;
    public final String description;
    public final int iconResId;
    public final String emoji;
    public final BadgeRarity rarity;
    public boolean earned;

    public Badge(String id, String name, String description, int iconResId,
                 String emoji, BadgeRarity rarity, boolean earned) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.iconResId = iconResId;
        this.emoji = emoji;
        this.rarity = rarity;
        this.earned = earned;
    }
}

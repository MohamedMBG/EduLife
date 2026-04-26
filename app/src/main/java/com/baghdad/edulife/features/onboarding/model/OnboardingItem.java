package com.baghdad.edulife.features.onboarding.model;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;

public class OnboardingItem {

    private final String title;
    private final String subtitle;
    private final String accentText;
    @DrawableRes private final int iconRes;
    @ColorRes private final int illustrationBgColor;

    public OnboardingItem(String title, String subtitle, String accentText,
                          @DrawableRes int iconRes, @ColorRes int illustrationBgColor) {
        this.title = title;
        this.subtitle = subtitle;
        this.accentText = accentText;
        this.iconRes = iconRes;
        this.illustrationBgColor = illustrationBgColor;
    }

    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public String getAccentText() { return accentText; }
    public int getIconRes() { return iconRes; }
    public int getIllustrationBgColor() { return illustrationBgColor; }
}

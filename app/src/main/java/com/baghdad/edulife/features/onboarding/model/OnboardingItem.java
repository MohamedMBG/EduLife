package com.baghdad.edulife.features.onboarding.model;

public class OnboardingItem {

    private final String title;
    private final String subtitle;
    private final String accentText;

    public OnboardingItem(String title, String subtitle, String accentText) {
        this.title = title;
        this.subtitle = subtitle;
        this.accentText = accentText;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getAccentText() {
        return accentText;
    }
}

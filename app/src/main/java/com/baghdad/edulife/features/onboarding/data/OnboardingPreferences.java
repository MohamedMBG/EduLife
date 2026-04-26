package com.baghdad.edulife.features.onboarding.data;

import android.content.Context;
import android.content.SharedPreferences;

public class OnboardingPreferences {

    private static final String PREFS_NAME = "edulife_onboarding";
    private static final String KEY_HAS_SEEN_ONBOARDING = "has_seen_onboarding";

    private final SharedPreferences preferences;

    public OnboardingPreferences(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean hasSeenOnboarding() {
        return preferences.getBoolean(KEY_HAS_SEEN_ONBOARDING, false);
    }

    public void markOnboardingSeen() {
        // This flag is local UI state only; authentication remains owned by the auth feature.
        preferences.edit()
                .putBoolean(KEY_HAS_SEEN_ONBOARDING, true)
                .apply();
    }
}

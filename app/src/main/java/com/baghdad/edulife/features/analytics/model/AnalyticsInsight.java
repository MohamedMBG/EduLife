package com.baghdad.edulife.features.analytics.model;

/**
 * A single human-readable insight line ("You are close to finishing Web Development"). Today these
 * are derived locally from the available data; a future backend may serve smarter insights.
 */
public class AnalyticsInsight {
    public final String message;

    public AnalyticsInsight(String message) {
        this.message = message;
    }
}

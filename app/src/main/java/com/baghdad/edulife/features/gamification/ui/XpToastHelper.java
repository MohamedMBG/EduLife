package com.baghdad.edulife.features.gamification.ui;

import android.content.Context;
import android.widget.Toast;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.gamification.data.GamificationPreferences;
import com.baghdad.edulife.features.gamification.data.XpEngine;
import com.baghdad.edulife.features.gamification.model.Badge;
import com.baghdad.edulife.features.gamification.model.XpAwardResult;
import com.baghdad.edulife.features.gamification.model.XpEvent;

public final class XpToastHelper {

    private XpToastHelper() {}

    public static XpAwardResult award(Context context, XpEvent event) {
        if (context == null || event == null) return null;
        Context appContext = context.getApplicationContext();
        XpEngine engine = new XpEngine(new GamificationPreferences(appContext));
        XpAwardResult result = engine.awardXp(event);
        showFeedback(appContext, event, result);
        return result;
    }

    public static void showFeedback(Context context, XpEvent event, XpAwardResult result) {
        if (context == null || result == null) return;
        if (result.xpAwarded > 0) {
            String label = labelFor(context, event);
            String msg = label.isEmpty()
                    ? context.getString(R.string.gamification_xp_toast_simple, result.xpAwarded)
                    : context.getString(R.string.gamification_xp_toast, result.xpAwarded, label);
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
        }
        if (result.didLevelUp && result.newLevelInfo != null) {
            Toast.makeText(context,
                    context.getString(R.string.gamification_levelup_toast, result.newLevelInfo.title),
                    Toast.LENGTH_LONG).show();
        }
        if (result.newBadges != null) {
            for (Badge b : result.newBadges) {
                Toast.makeText(context,
                        context.getString(R.string.gamification_badge_toast, b.name),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private static String labelFor(Context context, XpEvent event) {
        if (event == null) return "";
        switch (event) {
            case LESSON_COMPLETE:    return context.getString(R.string.gamification_event_lesson);
            case COURSE_COMPLETE:    return context.getString(R.string.gamification_event_course);
            case EXAM_PASS:          return context.getString(R.string.gamification_event_exam);
            case CERTIFICATE_EARNED: return context.getString(R.string.gamification_event_certificate);
            case ENROLLMENT:         return context.getString(R.string.gamification_event_enrollment);
            case STREAK_3_BONUS:     return context.getString(R.string.gamification_event_streak_3);
            case STREAK_7_BONUS:     return context.getString(R.string.gamification_event_streak_7);
            default:                 return "";
        }
    }
}

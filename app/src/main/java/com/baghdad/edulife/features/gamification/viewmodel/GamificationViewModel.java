package com.baghdad.edulife.features.gamification.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.baghdad.edulife.features.gamification.data.GamificationPreferences;
import com.baghdad.edulife.features.gamification.data.XpEngine;
import com.baghdad.edulife.features.gamification.model.Badge;
import com.baghdad.edulife.features.gamification.model.GamificationUiState;
import com.baghdad.edulife.features.gamification.model.LevelInfo;
import com.baghdad.edulife.features.gamification.model.XpAwardResult;
import com.baghdad.edulife.features.gamification.model.XpEvent;

import java.util.List;

/**
 * ViewModel for the gamification feature. Owns the GamificationPreferences and
 * XpEngine instances, exposing a single LiveData<GamificationUiState> for the UI.
 *
 * Uses AndroidViewModel so it can access the application context for SharedPreferences
 * without leaking an Activity reference.
 */
public class GamificationViewModel extends AndroidViewModel {

    private final GamificationPreferences prefs;
    private final XpEngine engine;

    private final MutableLiveData<GamificationUiState> _uiState = new MutableLiveData<>();
    public final LiveData<GamificationUiState> uiState = _uiState;

    /** Holds the most recent XP award result for the toast/snackbar to consume */
    private final MutableLiveData<XpAwardResult> _lastAward = new MutableLiveData<>();
    public final LiveData<XpAwardResult> lastAward = _lastAward;

    public GamificationViewModel(@NonNull Application application) {
        super(application);
        prefs = new GamificationPreferences(application);
        engine = new XpEngine(prefs);
        engine.reconcileBadges();
        refreshState();
    }

    /**
     * Awards XP for a given event, updates persisted state, and refreshes the UI LiveData.
     * Also emits the award result to lastAward so callers can show toast feedback.
     */
    public XpAwardResult awardXp(XpEvent event) {
        XpAwardResult result = engine.awardXp(event);
        _lastAward.postValue(result);
        refreshState();
        return result;
    }

    /** Re-reads all gamification state from preferences and emits a fresh UI state. */
    public void refreshState() {
        int totalXp = prefs.getTotalXp();
        LevelInfo levelInfo = engine.computeLevelInfo(totalXp);
        int streak = prefs.getStreak();
        List<Badge> badges = engine.getAllBadges();

        GamificationUiState state = new GamificationUiState(
                totalXp,
                levelInfo,
                streak,
                badges,
                prefs.getLessonsCompleted(),
                prefs.getCoursesEnrolled(),
                prefs.getCertificatesEarned()
        );
        _uiState.postValue(state);
    }

    /** Clears the last award so it is not re-consumed on config change. */
    public void clearLastAward() {
        _lastAward.setValue(null);
    }

    /** Convenience accessor for other fragments that need quick level info without full UI state. */
    public LevelInfo getCurrentLevelInfo() {
        return engine.computeLevelInfo(prefs.getTotalXp());
    }

    /** Convenience accessor for current streak count. */
    public int getCurrentStreak() {
        return prefs.getStreak();
    }

    /** Convenience accessor for total XP. */
    public int getTotalXp() {
        return prefs.getTotalXp();
    }
}

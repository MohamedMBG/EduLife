package com.baghdad.edulife.features.gamification.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.baghdad.edulife.features.analytics.data.AnalyticsRepository;
import com.baghdad.edulife.features.analytics.model.StudentAnalyticsSummary;
import com.baghdad.edulife.features.gamification.data.GamificationRepository;
import com.baghdad.edulife.features.gamification.model.Badge;
import com.baghdad.edulife.features.gamification.model.GamificationUiState;
import com.baghdad.edulife.features.gamification.model.LevelInfo;

/**
 * Backend-fetched gamification state. All progression numbers come from the
 * server; this ViewModel only orchestrates the request and exposes LiveData.
 *
 * No local XP / level / streak / badge math lives here — that rule is enforced
 * by the gamification spec in CLAUDE.md so a learner sees identical state on
 * Android and web.
 */
public class GamificationViewModel extends AndroidViewModel {

    private final GamificationRepository gamificationRepository;
    private final AnalyticsRepository analyticsRepository;

    private final MutableLiveData<GamificationUiState> _uiState = new MutableLiveData<>();
    public final LiveData<GamificationUiState> uiState = _uiState;

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public final LiveData<String> errorMessage = _errorMessage;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> isLoading = _isLoading;

    private GamificationUiState lastGamificationState;
    private StudentAnalyticsSummary lastAnalytics;

    public GamificationViewModel(@NonNull Application application) {
        super(application);
        this.gamificationRepository = new GamificationRepository();
        this.analyticsRepository = new AnalyticsRepository();
    }

    /**
     * Pulls the latest gamification state (and the matching analytics counters
     * used by the dashboard cards) from the backend. Safe to call on resume,
     * after enrol/lesson/exam/cert success, or on pull-to-refresh.
     */
    public void refreshState() {
        _isLoading.postValue(true);

        gamificationRepository.loadMyState(new GamificationRepository.StateCallback() {
            @Override
            public void onSuccess(GamificationUiState state) {
                lastGamificationState = state;
                emitMerged();
            }

            @Override
            public void onError(String message) {
                _isLoading.postValue(false);
                _errorMessage.postValue(message);
            }
        });

        analyticsRepository.loadStudentSummary(new AnalyticsRepository.StudentCallback() {
            @Override
            public void onSuccess(StudentAnalyticsSummary summary) {
                lastAnalytics = summary;
                emitMerged();
            }

            @Override
            public void onError(String message) {
                // Counters are a "nice to have" card on the gamification screen.
                // A failure here must not block the level / streak / badges that
                // already arrived from the gamification endpoint.
                emitMerged();
            }
        });
    }

    private void emitMerged() {
        if (lastGamificationState == null) {
            return;
        }
        int lessons = lastAnalytics != null ? (int) lastAnalytics.lessonsCompleted : 0;
        int courses = lastAnalytics != null ? (int) lastAnalytics.activeEnrollments : 0;
        int certs = lastAnalytics != null ? (int) lastAnalytics.certificatesEarned : 0;

        GamificationUiState merged = new GamificationUiState(
                lastGamificationState.totalXp,
                lastGamificationState.levelInfo,
                lastGamificationState.streak,
                lastGamificationState.badges,
                lessons,
                courses,
                certs
        );
        _uiState.postValue(merged);
        _isLoading.postValue(false);
    }

    /** Convenience accessor for callers that just want the cached level info. */
    public LevelInfo getCachedLevelInfo() {
        return lastGamificationState == null ? null : lastGamificationState.levelInfo;
    }

    /** Convenience accessor for the cached streak value. */
    public int getCachedStreak() {
        return lastGamificationState == null ? 0 : lastGamificationState.streak;
    }

    /** Convenience accessor for cached total XP. */
    public int getCachedTotalXp() {
        return lastGamificationState == null ? 0 : lastGamificationState.totalXp;
    }

    /** Returns true once any badges have been earned (server-reported). */
    public boolean hasAnyBadge() {
        if (lastGamificationState == null || lastGamificationState.badges == null) return false;
        for (Badge b : lastGamificationState.badges) {
            if (b.earned) return true;
        }
        return false;
    }
}

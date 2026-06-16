package com.baghdad.edulife.features.analytics.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.baghdad.edulife.features.analytics.data.AnalyticsRepository;
import com.baghdad.edulife.features.analytics.model.StudyAnalytics;
import com.baghdad.edulife.features.analytics.model.StudyAnalyticsUiState;

/**
 * Holds state for the redesigned Study Analytics screen. The fragment observes one uiState LiveData
 * and renders loading / error / success. The repository callback is posted off the main thread, so
 * success/error use postValue.
 */
public class StudyAnalyticsViewModel extends ViewModel {

    private final AnalyticsRepository repository;
    private final MutableLiveData<StudyAnalyticsUiState> uiState =
            new MutableLiveData<>(StudyAnalyticsUiState.loading());

    public StudyAnalyticsViewModel() {
        this.repository = new AnalyticsRepository();
    }

    public LiveData<StudyAnalyticsUiState> getUiState() {
        return uiState;
    }

    public void load() {
        // Always re-enter loading so a retry from an error state shows the spinner again.
        uiState.setValue(StudyAnalyticsUiState.loading());
        repository.loadStudyAnalytics(new AnalyticsRepository.StudyAnalyticsCallback() {
            @Override
            public void onSuccess(StudyAnalytics analytics) {
                uiState.postValue(StudyAnalyticsUiState.success(analytics));
            }

            @Override
            public void onError(String message) {
                uiState.postValue(StudyAnalyticsUiState.error(message));
            }
        });
    }
}

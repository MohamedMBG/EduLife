package com.baghdad.edulife.features.analytics.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.baghdad.edulife.features.analytics.data.AnalyticsRepository;
import com.baghdad.edulife.features.analytics.model.PlatformAnalytics;
import com.baghdad.edulife.features.analytics.model.PlatformAnalyticsUiState;
import com.baghdad.edulife.features.analytics.model.PlatformCohortAnalytics;
import com.baghdad.edulife.features.analytics.model.PlatformCohortUiState;

/** Holds platform analytics state for the admin overview screen. */
public class PlatformAnalyticsViewModel extends ViewModel {

    private final AnalyticsRepository repository;
    private final MutableLiveData<PlatformAnalyticsUiState> uiState =
            new MutableLiveData<>(PlatformAnalyticsUiState.loading());
    // Phase C cohort section loads independently of the headline counts.
    private final MutableLiveData<PlatformCohortUiState> cohortState =
            new MutableLiveData<>(PlatformCohortUiState.loading());

    public PlatformAnalyticsViewModel() {
        this.repository = new AnalyticsRepository();
    }

    public LiveData<PlatformAnalyticsUiState> getUiState() {
        return uiState;
    }

    public LiveData<PlatformCohortUiState> getCohortState() {
        return cohortState;
    }

    public void load() {
        uiState.setValue(PlatformAnalyticsUiState.loading());
        repository.loadPlatformAnalytics(new AnalyticsRepository.PlatformCallback() {
            @Override
            public void onSuccess(PlatformAnalytics analytics) {
                uiState.postValue(PlatformAnalyticsUiState.success(analytics));
            }

            @Override
            public void onError(String message) {
                uiState.postValue(PlatformAnalyticsUiState.error(message));
            }
        });
    }

    public void loadCohorts() {
        cohortState.setValue(PlatformCohortUiState.loading());
        repository.loadPlatformCohorts(new AnalyticsRepository.PlatformCohortCallback() {
            @Override
            public void onSuccess(PlatformCohortAnalytics analytics) {
                cohortState.postValue(PlatformCohortUiState.success(analytics));
            }

            @Override
            public void onError(String message) {
                cohortState.postValue(PlatformCohortUiState.error(message));
            }
        });
    }
}

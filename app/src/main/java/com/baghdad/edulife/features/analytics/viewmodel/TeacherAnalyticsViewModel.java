package com.baghdad.edulife.features.analytics.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.baghdad.edulife.features.analytics.data.AnalyticsRepository;
import com.baghdad.edulife.features.analytics.model.TeacherAnalytics;
import com.baghdad.edulife.features.analytics.model.TeacherAnalyticsUiState;

/** Holds teacher analytics state. Success may carry an empty course list (rendered as empty). */
public class TeacherAnalyticsViewModel extends ViewModel {

    private final AnalyticsRepository repository;
    private final MutableLiveData<TeacherAnalyticsUiState> uiState =
            new MutableLiveData<>(TeacherAnalyticsUiState.loading());

    public TeacherAnalyticsViewModel() {
        this.repository = new AnalyticsRepository();
    }

    public LiveData<TeacherAnalyticsUiState> getUiState() {
        return uiState;
    }

    public void load() {
        uiState.setValue(TeacherAnalyticsUiState.loading());
        repository.loadTeacherAnalytics(new AnalyticsRepository.TeacherCallback() {
            @Override
            public void onSuccess(TeacherAnalytics analytics) {
                uiState.postValue(TeacherAnalyticsUiState.success(analytics));
            }

            @Override
            public void onError(String message) {
                uiState.postValue(TeacherAnalyticsUiState.error(message));
            }
        });
    }
}

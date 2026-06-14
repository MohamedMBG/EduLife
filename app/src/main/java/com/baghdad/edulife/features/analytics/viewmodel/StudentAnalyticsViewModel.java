package com.baghdad.edulife.features.analytics.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.baghdad.edulife.features.analytics.data.AnalyticsRepository;
import com.baghdad.edulife.features.analytics.model.StudentAnalyticsSummary;
import com.baghdad.edulife.features.analytics.model.StudentAnalyticsUiState;
import com.baghdad.edulife.features.analytics.model.StudentProgressTrend;
import com.baghdad.edulife.features.analytics.model.StudentTrendUiState;

/**
 * Holds the student analytics screen state. The fragment observes a single uiState LiveData and
 * renders loading / error / success. State transitions: loading on load() start, then success or
 * error from the repository callback (posted off the main thread, hence postValue).
 */
public class StudentAnalyticsViewModel extends ViewModel {

    private final AnalyticsRepository repository;
    private final MutableLiveData<StudentAnalyticsUiState> uiState =
            new MutableLiveData<>(StudentAnalyticsUiState.loading());
    // Phase C: the monthly trend is a separate, independent section so a trend failure does not
    // blank out the summary (and vice versa). Two LiveData streams, two render paths.
    private final MutableLiveData<StudentTrendUiState> trendState =
            new MutableLiveData<>(StudentTrendUiState.loading());

    public StudentAnalyticsViewModel() {
        this.repository = new AnalyticsRepository();
    }

    public LiveData<StudentAnalyticsUiState> getUiState() {
        return uiState;
    }

    public LiveData<StudentTrendUiState> getTrendState() {
        return trendState;
    }

    public void load() {
        // Always re-enter loading so retry from an error state shows the spinner again.
        uiState.setValue(StudentAnalyticsUiState.loading());
        repository.loadStudentSummary(new AnalyticsRepository.StudentCallback() {
            @Override
            public void onSuccess(StudentAnalyticsSummary summary) {
                uiState.postValue(StudentAnalyticsUiState.success(summary));
            }

            @Override
            public void onError(String message) {
                uiState.postValue(StudentAnalyticsUiState.error(message));
            }
        });
    }

    public void loadTrend() {
        trendState.setValue(StudentTrendUiState.loading());
        repository.loadStudentTrend(new AnalyticsRepository.StudentTrendCallback() {
            @Override
            public void onSuccess(StudentProgressTrend trend) {
                trendState.postValue(StudentTrendUiState.success(trend));
            }

            @Override
            public void onError(String message) {
                trendState.postValue(StudentTrendUiState.error(message));
            }
        });
    }
}

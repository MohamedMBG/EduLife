package com.baghdad.edulife.features.advisor.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.baghdad.edulife.features.advisor.data.AdvisorRepository;
import com.baghdad.edulife.features.advisor.model.AdvisorResponse;
import com.baghdad.edulife.features.advisor.model.AdvisorUiState;

import java.util.Collections;

/**
 * ViewModel for the advisor screen that manages UI state and delegates recommendation
 * requests to {@link AdvisorRepository}, validating goal input before sending.
 */
public class AdvisorViewModel extends AndroidViewModel {

    private final AdvisorRepository repository;
    private final MutableLiveData<AdvisorUiState> uiState =
            new MutableLiveData<>(AdvisorUiState.idle());

    public AdvisorViewModel(@NonNull Application application) {
        super(application);
        this.repository = new AdvisorRepository();
    }

    public LiveData<AdvisorUiState> getUiState() {
        return uiState;
    }

    /**
     * Validates the goal (minimum 4 characters) and triggers an advisor recommendation request.
     * Updates UI state to loading, then to success or error based on the repository callback.
     */
    public void recommend(String rawGoal) {
        String goal = rawGoal == null ? "" : rawGoal.trim();
        if (goal.length() < 4) {
            uiState.setValue(AdvisorUiState.error(goal, "Write a clearer career goal first.", false));
            return;
        }

        uiState.setValue(AdvisorUiState.loading(goal));

        repository.recommend(goal, new AdvisorRepository.AdvisorCallback() {
            @Override
            public void onSuccess(AdvisorResponse response) {
                String message = response.message != null ? response.message : "";
                uiState.postValue(AdvisorUiState.success(
                        goal,
                        message,
                        response.recommendations != null ? response.recommendations : Collections.emptyList()
                ));
            }

            @Override
            public void onRateLimit() {
                uiState.postValue(AdvisorUiState.error(
                        goal,
                        "You've reached the hourly limit (10 requests). Try again later.",
                        true
                ));
            }

            @Override
            public void onError(String message) {
                uiState.postValue(AdvisorUiState.error(goal, message, false));
            }
        });
    }
}

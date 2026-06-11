package com.baghdad.edulife.features.admin.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.baghdad.edulife.features.admin.data.AdminRepository;
import com.baghdad.edulife.features.admin.model.AdminStats;
import com.baghdad.edulife.features.admin.model.AdminUiState;

public class AdminDashboardViewModel extends ViewModel {

    private final AdminRepository repository;
    private final MutableLiveData<AdminUiState> uiState =
            new MutableLiveData<>(AdminUiState.loading());

    public AdminDashboardViewModel() {
        this.repository = new AdminRepository();
    }

    public LiveData<AdminUiState> getUiState() {
        return uiState;
    }

    public void loadStats() {
        uiState.setValue(AdminUiState.loading());
        repository.loadStats(new AdminRepository.StatsCallback() {
            @Override
            public void onSuccess(AdminStats stats) {
                uiState.postValue(AdminUiState.success(stats));
            }
            @Override
            public void onError(String message) {
                uiState.postValue(AdminUiState.error(message));
            }
        });
    }
}

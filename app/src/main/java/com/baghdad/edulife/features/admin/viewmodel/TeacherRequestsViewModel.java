package com.baghdad.edulife.features.admin.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.baghdad.edulife.features.admin.data.AdminRepository;
import com.baghdad.edulife.features.admin.model.AdminTeacherRequest;
import com.baghdad.edulife.features.admin.model.TeacherRequestsUiState;

import java.util.ArrayList;
import java.util.List;

public class TeacherRequestsViewModel extends ViewModel {

    private final AdminRepository repository;
    private final MutableLiveData<TeacherRequestsUiState> uiState =
            new MutableLiveData<>(TeacherRequestsUiState.loading());

    private String currentFilter = "PENDING";

    public TeacherRequestsViewModel() {
        this.repository = new AdminRepository();
    }

    public LiveData<TeacherRequestsUiState> getUiState() {
        return uiState;
    }

    public String getCurrentFilter() {
        return currentFilter;
    }

    public void loadRequests(String status) {
        currentFilter = status;
        uiState.setValue(TeacherRequestsUiState.loading());
        repository.loadTeacherRequests(status, 0, 50, new AdminRepository.RequestsCallback() {
            @Override
            public void onSuccess(List<AdminTeacherRequest> requests) {
                uiState.postValue(TeacherRequestsUiState.success(requests));
            }
            @Override
            public void onError(String message) {
                uiState.postValue(TeacherRequestsUiState.error(message));
            }
        });
    }

    public void approveRequest(String requestId) {
        repository.approveRequest(requestId, new AdminRepository.ActionCallback() {
            @Override
            public void onSuccess(AdminTeacherRequest updated) {
                List<AdminTeacherRequest> current = currentRequests();
                current.removeIf(r -> requestId.equals(r.id));
                uiState.postValue(TeacherRequestsUiState.withAction(current, "Request approved — user promoted to Teacher."));
            }

            @Override
            public void onError(String message) {
                List<AdminTeacherRequest> current = currentRequests();
                uiState.postValue(TeacherRequestsUiState.withAction(current, "Approval failed: " + message));
            }
        });
    }

    public void rejectRequest(String requestId, String note) {
        repository.rejectRequest(requestId, note, new AdminRepository.ActionCallback() {
            @Override
            public void onSuccess(AdminTeacherRequest updated) {
                List<AdminTeacherRequest> current = currentRequests();
                current.removeIf(r -> requestId.equals(r.id));
                uiState.postValue(TeacherRequestsUiState.withAction(current, "Request rejected."));
            }

            @Override
            public void onError(String message) {
                List<AdminTeacherRequest> current = currentRequests();
                uiState.postValue(TeacherRequestsUiState.withAction(current, "Rejection failed: " + message));
            }
        });
    }

    private List<AdminTeacherRequest> currentRequests() {
        TeacherRequestsUiState state = uiState.getValue();
        if (state != null && state.requests != null) {
            return new ArrayList<>(state.requests);
        }
        return new ArrayList<>();
    }
}

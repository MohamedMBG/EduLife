package com.baghdad.edulife.features.groupadmin.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.baghdad.edulife.features.groupadmin.data.GroupAdminRepository;
import com.baghdad.edulife.features.groupadmin.model.GroupAdminUiState;
import com.baghdad.edulife.features.groupadmin.model.GroupSummary;

import java.util.List;

public class GroupAdminDashboardViewModel extends ViewModel {

    private final GroupAdminRepository repository;
    private final MutableLiveData<GroupAdminUiState> uiState =
            new MutableLiveData<>(GroupAdminUiState.loading());
    // One-shot message channel for create errors surfaced as a toast.
    private final MutableLiveData<String> message = new MutableLiveData<>();

    public GroupAdminDashboardViewModel() {
        this.repository = new GroupAdminRepository();
    }

    public LiveData<GroupAdminUiState> getUiState() {
        return uiState;
    }

    public LiveData<String> getMessage() {
        return message;
    }

    public void clearMessage() {
        message.setValue(null);
    }

    public void loadGroups() {
        uiState.setValue(GroupAdminUiState.loading());
        repository.loadGroups(new GroupAdminRepository.GroupsCallback() {
            @Override
            public void onSuccess(List<GroupSummary> groups) {
                uiState.postValue(GroupAdminUiState.success(groups));
            }

            @Override
            public void onError(String msg) {
                uiState.postValue(GroupAdminUiState.error(msg));
            }
        });
    }

    public void createGroup(String name) {
        repository.createGroup(name, new GroupAdminRepository.VoidCallback() {
            @Override
            public void onSuccess() {
                loadGroups();
            }

            @Override
            public void onError(String msg) {
                message.postValue(msg);
            }
        });
    }
}

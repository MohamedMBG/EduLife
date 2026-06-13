package com.baghdad.edulife.features.groupadmin.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.baghdad.edulife.features.groupadmin.data.GroupAdminRepository;
import com.baghdad.edulife.features.groupadmin.model.GroupDetail;
import com.baghdad.edulife.features.groupadmin.model.GroupDetailUiState;

public class GroupDetailViewModel extends ViewModel {

    private final GroupAdminRepository repository;
    private final MutableLiveData<GroupDetailUiState> uiState =
            new MutableLiveData<>(GroupDetailUiState.loading());
    private final MutableLiveData<String> message = new MutableLiveData<>();

    public GroupDetailViewModel() {
        this.repository = new GroupAdminRepository();
    }

    public LiveData<GroupDetailUiState> getUiState() {
        return uiState;
    }

    public LiveData<String> getMessage() {
        return message;
    }

    public void clearMessage() {
        message.setValue(null);
    }

    public void loadDetail(String groupId) {
        uiState.setValue(GroupDetailUiState.loading());
        repository.loadGroupDetail(groupId, new GroupAdminRepository.DetailCallback() {
            @Override
            public void onSuccess(GroupDetail detail) {
                uiState.postValue(GroupDetailUiState.success(detail));
            }

            @Override
            public void onError(String msg) {
                uiState.postValue(GroupDetailUiState.error(msg));
            }
        });
    }

    public void addMember(String groupId, String email) {
        repository.addMember(groupId, email, reloadOnSuccess(groupId));
    }

    public void removeMember(String groupId, String userId) {
        repository.removeMember(groupId, userId, reloadOnSuccess(groupId));
    }

    public void attachCourse(String groupId, String courseId) {
        repository.attachCourse(groupId, courseId, reloadOnSuccess(groupId));
    }

    /** Lets the fragment populate the assign-course picker without touching Retrofit itself. */
    public void loadCatalog(GroupAdminRepository.CatalogCallback callback) {
        repository.loadPublishedCatalog(callback);
    }

    /** Mutations reload the detail so the members/courses lists always reflect server state. */
    private GroupAdminRepository.VoidCallback reloadOnSuccess(String groupId) {
        return new GroupAdminRepository.VoidCallback() {
            @Override
            public void onSuccess() {
                loadDetail(groupId);
            }

            @Override
            public void onError(String msg) {
                message.postValue(msg);
            }
        };
    }
}

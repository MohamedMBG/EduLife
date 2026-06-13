package com.baghdad.edulife.features.groupadmin.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.baghdad.edulife.features.groupadmin.data.GroupAdminRepository;
import com.baghdad.edulife.features.groupadmin.model.ApprovalsUiState;
import com.baghdad.edulife.features.teacher.model.CmsCourse;

import java.util.ArrayList;
import java.util.List;

public class CourseApprovalsViewModel extends ViewModel {

    private final GroupAdminRepository repository;
    private final MutableLiveData<ApprovalsUiState> uiState =
            new MutableLiveData<>(ApprovalsUiState.loading());
    private final MutableLiveData<String> message = new MutableLiveData<>();

    public CourseApprovalsViewModel() {
        this.repository = new GroupAdminRepository();
    }

    public LiveData<ApprovalsUiState> getUiState() {
        return uiState;
    }

    public LiveData<String> getMessage() {
        return message;
    }

    public void clearMessage() {
        message.setValue(null);
    }

    public void load() {
        uiState.setValue(ApprovalsUiState.loading());
        repository.loadReviewQueue(new GroupAdminRepository.CoursesCallback() {
            @Override
            public void onSuccess(List<CmsCourse> courses) {
                // Split the queue into drafts awaiting approval and already-published courses.
                List<CmsCourse> pending = new ArrayList<>();
                List<CmsCourse> published = new ArrayList<>();
                for (CmsCourse c : courses) {
                    if ("PUBLISHED".equals(c.status)) {
                        published.add(c);
                    } else if (!"ARCHIVED".equals(c.status)) {
                        pending.add(c);
                    }
                }
                uiState.postValue(ApprovalsUiState.success(pending, published));
            }

            @Override
            public void onError(String msg) {
                uiState.postValue(ApprovalsUiState.error(msg));
            }
        });
    }

    public void approve(String courseId) {
        repository.publishCourse(courseId, new GroupAdminRepository.VoidCallback() {
            @Override
            public void onSuccess() {
                message.postValue("Course approved and published.");
                load();
            }

            @Override
            public void onError(String msg) {
                message.postValue(msg);
            }
        });
    }
}

package com.baghdad.edulife.features.profile.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.baghdad.edulife.features.profile.data.TeacherRequestRepository;
import com.baghdad.edulife.features.profile.model.TeacherRequestResponse;

public class TeacherRequestViewModel extends ViewModel {

    private final TeacherRequestRepository repository = new TeacherRequestRepository();

    private final MutableLiveData<TeacherRequestResponse> latestRequest = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> submitting = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<String> submitMessage = new MutableLiveData<>();

    public LiveData<TeacherRequestResponse> getLatestRequest() {
        return latestRequest;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Boolean> getSubmitting() {
        return submitting;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<String> getSubmitMessage() {
        return submitMessage;
    }

    public void loadLatestRequest() {
        loading.setValue(true);
        repository.getMyLatestRequest(new TeacherRequestRepository.LatestRequestCallback() {
            @Override
            public void onSuccess(TeacherRequestResponse request) {
                loading.postValue(false);
                latestRequest.postValue(request);
            }

            @Override
            public void onEmpty() {
                loading.postValue(false);
                latestRequest.postValue(null);
            }

            @Override
            public void onError(String message) {
                loading.postValue(false);
                error.postValue(message);
            }
        });
    }

    public void submitTeacherRequest(String motivation) {
        submitting.setValue(true);
        repository.submitTeacherRequest(motivation, new TeacherRequestRepository.SubmitCallback() {
            @Override
            public void onSuccess(TeacherRequestResponse request) {
                submitting.postValue(false);
                latestRequest.postValue(request);
                submitMessage.postValue("Teacher request submitted.");
            }

            @Override
            public void onAlreadyPending() {
                submitting.postValue(false);
                submitMessage.postValue("A pending teacher request already exists.");
                loadLatestRequest();
            }

            @Override
            public void onError(String message) {
                submitting.postValue(false);
                error.postValue(message);
            }
        });
    }

    public void clearError() {
        error.setValue(null);
    }

    public void clearSubmitMessage() {
        submitMessage.setValue(null);
    }
}

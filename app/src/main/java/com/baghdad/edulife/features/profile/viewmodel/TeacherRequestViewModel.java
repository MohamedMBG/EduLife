package com.baghdad.edulife.features.profile.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.baghdad.edulife.features.profile.data.TeacherRequestRepository;
import com.baghdad.edulife.features.profile.model.SubmitTeacherRequestBody;
import com.baghdad.edulife.features.profile.model.TeacherRequestResponse;

public class TeacherRequestViewModel extends ViewModel {

    private final TeacherRequestRepository repository = new TeacherRequestRepository();

    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(false);
    public final LiveData<Boolean> loading = _loading;

    private final MutableLiveData<TeacherRequestResponse> _request = new MutableLiveData<>();
    public final LiveData<TeacherRequestResponse> request = _request;

    // True when the backend confirmed no request exists (204 response).
    private final MutableLiveData<Boolean> _noRequest = new MutableLiveData<>(false);
    public final LiveData<Boolean> noRequest = _noRequest;

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public final LiveData<String> error = _error;

    private final MutableLiveData<Boolean> _submitting = new MutableLiveData<>(false);
    public final LiveData<Boolean> submitting = _submitting;

    private final MutableLiveData<String> _submitError = new MutableLiveData<>();
    public final LiveData<String> submitError = _submitError;

    private boolean loaded = false;

    public void load() {
        if (loaded) return;
        loaded = true;
        _loading.setValue(true);
        repository.getMyRequest(new TeacherRequestRepository.GetRequestCallback() {
            @Override
            public void onSuccess(TeacherRequestResponse req) {
                _loading.postValue(false);
                if (req == null) {
                    _noRequest.postValue(true);
                } else {
                    _noRequest.postValue(false);
                    _request.postValue(req);
                }
            }

            @Override
            public void onError(String message) {
                _loading.postValue(false);
                _error.postValue(message);
            }
        });
    }

    public void submit(String motivation) {
        _submitting.setValue(true);
        _submitError.setValue(null);
        String trimmed = motivation != null ? motivation.trim() : null;
        SubmitTeacherRequestBody body = new SubmitTeacherRequestBody(
                trimmed != null && !trimmed.isEmpty() ? trimmed : null);
        repository.submitRequest(body, new TeacherRequestRepository.SubmitCallback() {
            @Override
            public void onSuccess(TeacherRequestResponse req) {
                _submitting.postValue(false);
                _noRequest.postValue(false);
                _request.postValue(req);
            }

            @Override
            public void onError(String message) {
                _submitting.postValue(false);
                _submitError.postValue(message);
            }
        });
    }

    public void clearSubmitError() {
        _submitError.setValue(null);
    }

    public void reload() {
        loaded = false;
        _noRequest.setValue(false);
        _request.setValue(null);
        _error.setValue(null);
        load();
    }
}

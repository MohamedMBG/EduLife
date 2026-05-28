package com.baghdad.edulife.features.profile.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.baghdad.edulife.features.profile.data.ProfileRepository;
import com.baghdad.edulife.features.profile.model.ProfileResponse;

public class ProfileViewModel extends ViewModel {

    private final ProfileRepository repository = new ProfileRepository();

    private final MutableLiveData<ProfileResponse> _profile = new MutableLiveData<>();
    public final LiveData<ProfileResponse> profile = _profile;

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public final LiveData<String> error = _error;

    // Single-shot signal so the fragment can drive sign-out + nav exactly once per success.
    private final MutableLiveData<Boolean> _accountDeleted = new MutableLiveData<>();
    public final LiveData<Boolean> accountDeleted = _accountDeleted;

    private final MutableLiveData<Boolean> _deleting = new MutableLiveData<>(false);
    public final LiveData<Boolean> deleting = _deleting;

    // Separate error stream so delete failures stay distinct from profile-load failures.
    private final MutableLiveData<String> _deleteError = new MutableLiveData<>();
    public final LiveData<String> deleteError = _deleteError;

    public void loadProfile() {
        repository.loadProfile(new ProfileRepository.ProfileCallback() {
            @Override
            public void onSuccess(ProfileResponse profileResponse) {
                _profile.postValue(profileResponse);
            }

            @Override
            public void onError(String message) {
                _error.postValue(message);
            }
        });
    }

    public void deleteAccount() {
        _deleting.postValue(true);
        repository.deleteAccount(new ProfileRepository.DeleteAccountCallback() {
            @Override
            public void onSuccess() {
                _deleting.postValue(false);
                _accountDeleted.postValue(true);
            }

            @Override
            public void onError(String message) {
                _deleting.postValue(false);
                _deleteError.postValue(message);
            }
        });
    }
}

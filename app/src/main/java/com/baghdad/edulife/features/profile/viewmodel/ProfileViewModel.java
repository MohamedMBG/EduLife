package com.baghdad.edulife.features.profile.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.baghdad.edulife.features.profile.data.ProfileRepository;
import com.baghdad.edulife.features.profile.model.ProfileResponse;
import com.baghdad.edulife.features.profile.model.UpdateProfileRequest;

import java.io.File;

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

    private final MutableLiveData<Boolean> _saving = new MutableLiveData<>(false);
    public final LiveData<Boolean> saving = _saving;

    private final MutableLiveData<Boolean> _saveSuccess = new MutableLiveData<>();
    public final LiveData<Boolean> saveSuccess = _saveSuccess;

    private final MutableLiveData<String> _saveError = new MutableLiveData<>();
    public final LiveData<String> saveError = _saveError;

    private final MutableLiveData<Boolean> _uploading = new MutableLiveData<>(false);
    public final LiveData<Boolean> uploading = _uploading;

    private final MutableLiveData<String> _uploadedAvatarUrl = new MutableLiveData<>();
    public final LiveData<String> uploadedAvatarUrl = _uploadedAvatarUrl;

    private final MutableLiveData<String> _uploadError = new MutableLiveData<>();
    public final LiveData<String> uploadError = _uploadError;

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

    /**
     * Consumed by the fragment after sign-out + nav so a stale "true" cannot re-fire the
     * sign-out branch if the same VM instance is observed again later.
     */
    public void clearAccountDeleted() {
        _accountDeleted.setValue(null);
    }

    public void clearDeleteError() {
        _deleteError.setValue(null);
    }

    public void updateProfile(String displayName, String bio) {
        _saving.setValue(true);
        _saveError.setValue(null);
        repository.updateProfile(new UpdateProfileRequest(displayName, bio),
                new ProfileRepository.UpdateProfileCallback() {
                    @Override
                    public void onSuccess(ProfileResponse updated) {
                        _saving.postValue(false);
                        _profile.postValue(updated);
                        _saveSuccess.postValue(true);
                    }

                    @Override
                    public void onError(String message) {
                        _saving.postValue(false);
                        _saveError.postValue(message);
                    }
                });
    }

    public void clearSaveSuccess() {
        _saveSuccess.setValue(null);
    }

    public void clearSaveError() {
        _saveError.setValue(null);
    }

    public void uploadAvatar(File imageFile) {
        // Drop overlapping requests: rapid taps on the avatar picker callback otherwise produce
        // N concurrent multipart uploads that all race to update the server-side avatar URL.
        // The in-flight flag is the single source of truth so the fragment does not need its own
        // disabled-state tracking.
        if (Boolean.TRUE.equals(_uploading.getValue())) return;

        _uploading.setValue(true);
        _uploadError.setValue(null);
        repository.uploadAvatar(imageFile, new ProfileRepository.UploadAvatarCallback() {
            @Override
            public void onSuccess(String avatarUrl) {
                _uploading.postValue(false);
                _uploadedAvatarUrl.postValue(avatarUrl);
                loadProfile();
            }

            @Override
            public void onError(String message) {
                _uploading.postValue(false);
                _uploadError.postValue(message);
            }
        });
    }

    public void clearUploadError() {
        _uploadError.setValue(null);
    }
}

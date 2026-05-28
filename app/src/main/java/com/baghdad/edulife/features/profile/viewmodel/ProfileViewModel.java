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

    private final MutableLiveData<Boolean> _saving = new MutableLiveData<>(false);
    public final LiveData<Boolean> saving = _saving;

    private final MutableLiveData<String> _saveMessage = new MutableLiveData<>();
    public final LiveData<String> saveMessage = _saveMessage;

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

    public void updateProfile(String displayName, String bio) {
        _saving.setValue(true);

        repository.updateProfile(displayName, bio, new ProfileRepository.ProfileCallback() {
            @Override
            public void onSuccess(ProfileResponse profileResponse) {
                _saving.postValue(false);
                _profile.postValue(profileResponse);
                _saveMessage.postValue("Profile updated successfully.");
            }

            @Override
            public void onError(String message) {
                _saving.postValue(false);
                _error.postValue(message);
            }
        });
    }
}

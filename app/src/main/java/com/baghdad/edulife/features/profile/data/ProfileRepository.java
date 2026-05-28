package com.baghdad.edulife.features.profile.data;

import androidx.annotation.NonNull;

import com.baghdad.edulife.core.network.ApiClient;
import com.baghdad.edulife.core.network.ApiService;
import com.baghdad.edulife.features.profile.model.ProfileResponse;
import com.baghdad.edulife.features.profile.model.UpdateProfileRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileRepository {

    public interface ProfileCallback {
        void onSuccess(ProfileResponse profile);
        void onError(String message);
    }

    private final ApiService apiService;

    public ProfileRepository() {
        this.apiService = ApiClient.getClient().create(ApiService.class);
    }

    public void loadProfile(ProfileCallback callback) {
        apiService.getProfile().enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<ProfileResponse> call,
                                   @NonNull Response<ProfileResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Profile load failed. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<ProfileResponse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    public void updateProfile(String displayName, String bio, ProfileCallback callback) {
        apiService.updateProfile(new UpdateProfileRequest(displayName, bio)).enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<ProfileResponse> call,
                                   @NonNull Response<ProfileResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Profile update failed. Status: " + response.code());
                    return;
                }

                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<ProfileResponse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }
}

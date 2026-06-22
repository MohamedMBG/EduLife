package com.baghdad.edulife.features.profile.data;

import androidx.annotation.NonNull;

import com.baghdad.edulife.core.network.ApiClient;
import com.baghdad.edulife.core.network.ApiService;
import com.baghdad.edulife.features.profile.model.AvatarUploadResponse;
import com.baghdad.edulife.features.profile.model.ProfileResponse;
import com.baghdad.edulife.features.profile.model.UpdateProfileRequest;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileRepository {

    public interface ProfileCallback {
        void onSuccess(ProfileResponse profile);
        void onError(String message);
    }

    public interface DeleteAccountCallback {
        void onSuccess();
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

    public interface UpdateProfileCallback {
        void onSuccess(ProfileResponse profile);
        void onError(String message);
    }

    public interface UploadAvatarCallback {
        void onSuccess(String avatarUrl);
        void onError(String message);
    }

    public void updateProfile(UpdateProfileRequest request, UpdateProfileCallback callback) {
        apiService.updateProfile(request).enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<ProfileResponse> call,
                                   @NonNull Response<ProfileResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Update failed. Status: " + response.code());
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

    public void uploadAvatar(File imageFile, String mimeType, UploadAvatarCallback callback) {
        RequestBody reqBody = RequestBody.create(imageFile, MediaType.parse(mimeType));
        
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", imageFile.getName(), reqBody);
        apiService.uploadAvatar(part).enqueue(new Callback<AvatarUploadResponse>() {
            @Override
            public void onResponse(@NonNull Call<AvatarUploadResponse> call,
                                   @NonNull Response<AvatarUploadResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Avatar upload failed. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body().avatarUrl);
            }

            @Override
            public void onFailure(@NonNull Call<AvatarUploadResponse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    public void deleteAccount(DeleteAccountCallback callback) {
        apiService.deleteAccount().enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                    return;
                }
                callback.onError("Account deletion failed. Status: " + response.code());
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }
}

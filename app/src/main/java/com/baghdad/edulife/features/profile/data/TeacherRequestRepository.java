package com.baghdad.edulife.features.profile.data;

import androidx.annotation.NonNull;

import com.baghdad.edulife.core.network.ApiClient;
import com.baghdad.edulife.core.network.ApiService;
import com.baghdad.edulife.features.profile.model.SubmitTeacherRequestBody;
import com.baghdad.edulife.features.profile.model.TeacherRequestResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TeacherRequestRepository {

    public interface GetRequestCallback {
        void onSuccess(TeacherRequestResponse request); // null = 204, no request exists
        void onError(String message);
    }

    public interface SubmitCallback {
        void onSuccess(TeacherRequestResponse request);
        void onError(String message);
    }

    private final ApiService apiService;

    public TeacherRequestRepository() {
        this.apiService = ApiClient.getClient().create(ApiService.class);
    }

    public void getMyRequest(GetRequestCallback callback) {
        apiService.getMyTeacherRequest().enqueue(new Callback<TeacherRequestResponse>() {
            @Override
            public void onResponse(@NonNull Call<TeacherRequestResponse> call,
                                   @NonNull Response<TeacherRequestResponse> response) {
                if (response.code() == 204) {
                    callback.onSuccess(null);
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Request load failed. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<TeacherRequestResponse> call,
                                  @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    public void submitRequest(SubmitTeacherRequestBody body, SubmitCallback callback) {
        apiService.submitTeacherRequest(body).enqueue(new Callback<TeacherRequestResponse>() {
            @Override
            public void onResponse(@NonNull Call<TeacherRequestResponse> call,
                                   @NonNull Response<TeacherRequestResponse> response) {
                if (response.code() == 409) {
                    callback.onError("You already have a pending request.");
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Submission failed. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<TeacherRequestResponse> call,
                                  @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }
}

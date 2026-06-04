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

    public interface LatestRequestCallback {
        void onSuccess(TeacherRequestResponse request);
        void onEmpty();
        void onError(String message);
    }

    public interface SubmitCallback {
        void onSuccess(TeacherRequestResponse request);
        void onAlreadyPending();
        void onError(String message);
    }

    private final ApiService apiService;

    public TeacherRequestRepository() {
        this.apiService = ApiClient.getClient().create(ApiService.class);
    }

    public void getMyLatestRequest(LatestRequestCallback callback) {
        apiService.getMyTeacherRequest().enqueue(new Callback<TeacherRequestResponse>() {
            @Override
            public void onResponse(@NonNull Call<TeacherRequestResponse> call,
                                   @NonNull Response<TeacherRequestResponse> response) {
                if (response.code() == 204) {
                    callback.onEmpty();
                    return;
                }
                if (!response.isSuccessful()) {
                    callback.onError("Teacher request load failed. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<TeacherRequestResponse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    public void submitTeacherRequest(String motivation, SubmitCallback callback) {
        apiService.submitTeacherRequest(new SubmitTeacherRequestBody(motivation))
                .enqueue(new Callback<TeacherRequestResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<TeacherRequestResponse> call,
                                           @NonNull Response<TeacherRequestResponse> response) {
                        if (response.code() == 409) {
                            callback.onAlreadyPending();
                            return;
                        }
                        if (!response.isSuccessful() || response.body() == null) {
                            callback.onError("Teacher request submission failed. Status: " + response.code());
                            return;
                        }
                        callback.onSuccess(response.body());
                    }

                    @Override
                    public void onFailure(@NonNull Call<TeacherRequestResponse> call, @NonNull Throwable t) {
                        callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
                    }
                });
    }
}

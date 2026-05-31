package com.baghdad.edulife.features.courses.data;

import com.baghdad.edulife.core.network.ApiClient;
import com.baghdad.edulife.core.network.ApiService;
import com.baghdad.edulife.features.courses.model.ExamCooldownError;
import com.baghdad.edulife.features.courses.model.ExamResponse;
import com.baghdad.edulife.features.courses.model.ExamResultResponse;
import com.baghdad.edulife.features.courses.model.SubmitExamRequest;
import com.google.gson.Gson;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExamRepository {

    private final ApiService apiService;

    public ExamRepository() {
        this.apiService = ApiClient.getClient().create(ApiService.class);
    }

    public interface ExamCallback {
        void onSuccess(ExamResponse exam);
        void onError(String message);
    }

    public interface SubmitCallback {
        void onSuccess(ExamResultResponse result);
        void onAlreadyPassed();
        void onCooldown(String cooldownEndsAt);
        void onError(String message);
    }

    public void getExam(String courseId, ExamCallback callback) {
        apiService.getExam(courseId).enqueue(new Callback<ExamResponse>() {
            @Override
            public void onResponse(Call<ExamResponse> call, Response<ExamResponse> response) {
                if (response.code() == 403) {
                    callback.onError("You must be enrolled to take this exam.");
                    return;
                }
                if (response.code() == 404) {
                    callback.onError("No exam found for this course.");
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Failed to load exam. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(Call<ExamResponse> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void submitExam(String courseId, SubmitExamRequest request, SubmitCallback callback) {
        apiService.submitExam(courseId, request).enqueue(new Callback<ExamResultResponse>() {
            @Override
            public void onResponse(Call<ExamResultResponse> call, Response<ExamResultResponse> response) {
                if (response.code() == 403) {
                    callback.onError("You must be enrolled to submit this exam.");
                    return;
                }
                if (response.code() == 409) {
                    callback.onAlreadyPassed();
                    return;
                }
                if (response.code() == 429) {
                    String cooldownEndsAt = null;
                    if (response.errorBody() != null) {
                        try {
                            String body = response.errorBody().string();
                            ExamCooldownError err = new Gson().fromJson(body, ExamCooldownError.class);
                            if (err != null) cooldownEndsAt = err.cooldownEndsAt;
                        } catch (IOException ignored) {}
                    }
                    callback.onCooldown(cooldownEndsAt);
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Failed to submit exam. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(Call<ExamResultResponse> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
}

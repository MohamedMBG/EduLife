package com.baghdad.edulife.features.exam.data;

import androidx.annotation.NonNull;

import com.baghdad.edulife.core.network.ApiClient;
import com.baghdad.edulife.core.network.ApiService;
import com.baghdad.edulife.features.exam.model.ExamDto;
import com.baghdad.edulife.features.exam.model.ExamResultDto;
import com.baghdad.edulife.features.exam.model.SubmitExamRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExamRepository {

    private final ApiService apiService;

    public ExamRepository() {
        this.apiService = ApiClient.getClient().create(ApiService.class);
    }

    public interface ExamCallback {
        void onSuccess(ExamDto exam);
        void onError(String message);
    }

    public interface ExamResultCallback {
        void onSuccess(ExamResultDto result);
        void onError(String message);
    }

    public void loadExam(String courseId, ExamCallback callback) {
        apiService.getExam(courseId).enqueue(new Callback<ExamDto>() {
            @Override
            public void onResponse(@NonNull Call<ExamDto> call, @NonNull Response<ExamDto> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Failed to load exam. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<ExamDto> call, @NonNull Throwable t) {
                callback.onError("Network error: " + safeMessage(t));
            }
        });
    }

    public void submitExam(String courseId, SubmitExamRequest request, ExamResultCallback callback) {
        apiService.submitExam(courseId, request).enqueue(new Callback<ExamResultDto>() {
            @Override
            public void onResponse(@NonNull Call<ExamResultDto> call, @NonNull Response<ExamResultDto> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Exam submission failed. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<ExamResultDto> call, @NonNull Throwable t) {
                callback.onError("Network error: " + safeMessage(t));
            }
        });
    }

    private String safeMessage(Throwable t) {
        return t.getMessage() != null && !t.getMessage().isBlank() ? t.getMessage() : "Unknown error";
    }
}

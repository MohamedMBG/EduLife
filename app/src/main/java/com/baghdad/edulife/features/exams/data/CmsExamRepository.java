package com.baghdad.edulife.features.exams.data;

import androidx.annotation.NonNull;

import com.baghdad.edulife.core.network.ApiClient;
import com.baghdad.edulife.core.network.ApiService;
import com.baghdad.edulife.features.exams.model.CmsExamRequest;
import com.baghdad.edulife.features.exams.model.CmsExamResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CmsExamRepository {

    private final ApiService apiService;

    public CmsExamRepository() {
        this.apiService = ApiClient.getClient().create(ApiService.class);
    }

    public interface ExamCallback {
        void onSuccess(CmsExamResponse exam);
        void onNotFound();
        void onAccessDenied(String message);
        void onError(String message);
    }

    public interface CreateExamCallback {
        void onSuccess(CmsExamResponse exam);
        void onConflict();
        void onAccessDenied(String message);
        void onError(String message);
    }

    public void getCourseExam(String courseId, ExamCallback callback) {
        apiService.getCmsCourseExam(courseId).enqueue(new Callback<CmsExamResponse>() {
            @Override
            public void onResponse(@NonNull Call<CmsExamResponse> call,
                                   @NonNull Response<CmsExamResponse> response) {
                if (response.code() == 404) {
                    callback.onNotFound();
                    return;
                }
                if (response.code() == 403) {
                    callback.onAccessDenied("You don't have permission to manage this course's exam.");
                    return;
                }
                if (response.code() == 401) {
                    callback.onAccessDenied("Session expired. Please sign in again.");
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Failed to load exam. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<CmsExamResponse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    public void createCourseExam(String courseId, CmsExamRequest request,
                                 CreateExamCallback callback) {
        apiService.createCmsCourseExam(courseId, request).enqueue(new Callback<CmsExamResponse>() {
            @Override
            public void onResponse(@NonNull Call<CmsExamResponse> call,
                                   @NonNull Response<CmsExamResponse> response) {
                if (response.code() == 409) {
                    callback.onConflict();
                    return;
                }
                if (response.code() == 403) {
                    callback.onAccessDenied(
                            "You don't have permission to create an exam for this course.");
                    return;
                }
                if (response.code() == 401) {
                    callback.onAccessDenied("Session expired. Please sign in again.");
                    return;
                }
                if (response.code() == 400) {
                    callback.onError("Validation error. Check all fields and try again.");
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Failed to create exam. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<CmsExamResponse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }
}

package com.baghdad.edulife.features.admin.data;

import androidx.annotation.NonNull;

import com.baghdad.edulife.core.network.ApiClient;
import com.baghdad.edulife.core.network.ApiService;
import com.baghdad.edulife.features.admin.model.AdminPageResponse;
import com.baghdad.edulife.features.admin.model.AdminRejectRequest;
import com.baghdad.edulife.features.admin.model.AdminStats;
import com.baghdad.edulife.features.admin.model.AdminTeacherRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminRepository {

    public interface StatsCallback {
        void onSuccess(AdminStats stats);
        void onError(String message);
    }

    public interface RequestsCallback {
        void onSuccess(List<AdminTeacherRequest> requests);
        void onError(String message);
    }

    public interface ActionCallback {
        void onSuccess(AdminTeacherRequest updated);
        void onError(String message);
    }

    private final ApiService apiService;

    public AdminRepository() {
        this.apiService = ApiClient.getClient().create(ApiService.class);
    }

    public void loadStats(StatsCallback callback) {
        apiService.getAdminStats().enqueue(new Callback<AdminStats>() {
            @Override
            public void onResponse(@NonNull Call<AdminStats> call,
                                   @NonNull Response<AdminStats> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Failed to load stats. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<AdminStats> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    public void loadTeacherRequests(String status, int page, int size, RequestsCallback callback) {
        apiService.getAdminTeacherRequests(status, page, size)
                .enqueue(new Callback<AdminPageResponse<AdminTeacherRequest>>() {
                    @Override
                    public void onResponse(@NonNull Call<AdminPageResponse<AdminTeacherRequest>> call,
                                           @NonNull Response<AdminPageResponse<AdminTeacherRequest>> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            callback.onError("Failed to load requests. Status: " + response.code());
                            return;
                        }
                        List<AdminTeacherRequest> items = response.body().content;
                        callback.onSuccess(items != null ? items : java.util.Collections.emptyList());
                    }

                    @Override
                    public void onFailure(@NonNull Call<AdminPageResponse<AdminTeacherRequest>> call,
                                          @NonNull Throwable t) {
                        callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
                    }
                });
    }

    public void approveRequest(String requestId, ActionCallback callback) {
        apiService.approveTeacherRequest(requestId).enqueue(new Callback<AdminTeacherRequest>() {
            @Override
            public void onResponse(@NonNull Call<AdminTeacherRequest> call,
                                   @NonNull Response<AdminTeacherRequest> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Approval failed. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<AdminTeacherRequest> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    public void rejectRequest(String requestId, String note, ActionCallback callback) {
        apiService.rejectTeacherRequest(requestId, new AdminRejectRequest(note))
                .enqueue(new Callback<AdminTeacherRequest>() {
                    @Override
                    public void onResponse(@NonNull Call<AdminTeacherRequest> call,
                                           @NonNull Response<AdminTeacherRequest> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            callback.onError("Rejection failed. Status: " + response.code());
                            return;
                        }
                        callback.onSuccess(response.body());
                    }

                    @Override
                    public void onFailure(@NonNull Call<AdminTeacherRequest> call, @NonNull Throwable t) {
                        callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
                    }
                });
    }
}

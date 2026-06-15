package com.baghdad.edulife.features.analytics.data;

import androidx.annotation.NonNull;

import com.baghdad.edulife.core.network.ApiClient;
import com.baghdad.edulife.core.network.ApiService;
import com.baghdad.edulife.features.analytics.model.PlatformAnalytics;
import com.baghdad.edulife.features.analytics.model.PlatformCohortAnalytics;
import com.baghdad.edulife.features.analytics.model.StudentAnalyticsSummary;
import com.baghdad.edulife.features.analytics.model.StudentProgressTrend;
import com.baghdad.edulife.features.analytics.model.TeacherAnalytics;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Data access for the read-only analytics endpoints. Mirrors the existing AdminRepository pattern:
 * Retrofit calls via the shared ApiClient, results delivered through typed callbacks.
 *
 * Auth: the Firebase Bearer token is injected by FirebaseAuthInterceptor and a single 401
 * refresh-and-retry is performed by FirebaseTokenAuthenticator at the OkHttp layer, so no token
 * handling is needed here. A persistent 401/403 surfaces as an error message to the ViewModel.
 */
public class AnalyticsRepository {

    public interface StudentCallback {
        void onSuccess(StudentAnalyticsSummary summary);
        void onError(String message);
    }

    public interface TeacherCallback {
        void onSuccess(TeacherAnalytics analytics);
        void onError(String message);
    }

    public interface PlatformCallback {
        void onSuccess(PlatformAnalytics analytics);
        void onError(String message);
    }

    public interface StudentTrendCallback {
        void onSuccess(StudentProgressTrend trend);
        void onError(String message);
    }

    public interface PlatformCohortCallback {
        void onSuccess(PlatformCohortAnalytics analytics);
        void onError(String message);
    }

    private final ApiService apiService;

    public AnalyticsRepository() {
        this.apiService = ApiClient.getClient().create(ApiService.class);
    }

    /** Loads the caller's own student summary. */
    public void loadStudentSummary(StudentCallback callback) {
        apiService.getMyAnalyticsSummary().enqueue(new Callback<StudentAnalyticsSummary>() {
            @Override
            public void onResponse(@NonNull Call<StudentAnalyticsSummary> call,
                                   @NonNull Response<StudentAnalyticsSummary> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Failed to load your stats. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<StudentAnalyticsSummary> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    /** Loads the caller's owned-course analytics (server scopes to the teacher's own courses). */
    public void loadTeacherAnalytics(TeacherCallback callback) {
        apiService.getTeacherAnalytics().enqueue(new Callback<TeacherAnalytics>() {
            @Override
            public void onResponse(@NonNull Call<TeacherAnalytics> call,
                                   @NonNull Response<TeacherAnalytics> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Failed to load course analytics. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<TeacherAnalytics> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    /** Loads global platform analytics (ADMIN only, enforced server-side). */
    public void loadPlatformAnalytics(PlatformCallback callback) {
        apiService.getPlatformAnalytics().enqueue(new Callback<PlatformAnalytics>() {
            @Override
            public void onResponse(@NonNull Call<PlatformAnalytics> call,
                                   @NonNull Response<PlatformAnalytics> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Failed to load platform analytics. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<PlatformAnalytics> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    /** Loads the caller's own lessons-completed-per-month trend. */
    public void loadStudentTrend(StudentTrendCallback callback) {
        apiService.getMyProgressTrend().enqueue(new Callback<StudentProgressTrend>() {
            @Override
            public void onResponse(@NonNull Call<StudentProgressTrend> call,
                                   @NonNull Response<StudentProgressTrend> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Failed to load progress trend. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<StudentProgressTrend> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    /** Loads global cohort analytics (ADMIN only, enforced server-side). */
    public void loadPlatformCohorts(PlatformCohortCallback callback) {
        apiService.getPlatformCohorts().enqueue(new Callback<PlatformCohortAnalytics>() {
            @Override
            public void onResponse(@NonNull Call<PlatformCohortAnalytics> call,
                                   @NonNull Response<PlatformCohortAnalytics> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Failed to load cohort analytics. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<PlatformCohortAnalytics> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }
}

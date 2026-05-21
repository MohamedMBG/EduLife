package com.baghdad.edulife.features.courses.data;

import androidx.annotation.NonNull;

import com.baghdad.edulife.core.network.ApiClient;
import com.baghdad.edulife.core.network.ApiService;
import com.baghdad.edulife.features.courses.model.CourseDetail;
import com.baghdad.edulife.features.courses.model.CoursePageResponse;
import com.baghdad.edulife.features.courses.model.CourseSummary;

import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CourseRepository {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final ApiService apiService;

    public CourseRepository() {
        this.apiService = ApiClient.getClient().create(ApiService.class);
    }

    public interface CourseCatalogCallback {
        void onSuccess(List<CourseSummary> courses);
        void onError(String message);
    }

    public interface CourseDetailCallback {
        void onSuccess(CourseDetail courseDetail);
        void onError(String message);
    }

    public void loadCourses(String category, int page, CourseCatalogCallback callback) {
        apiService.getCourses(category, page, DEFAULT_PAGE_SIZE).enqueue(new Callback<CoursePageResponse<CourseSummary>>() {
            @Override
            public void onResponse(
                    @NonNull Call<CoursePageResponse<CourseSummary>> call,
                    @NonNull Response<CoursePageResponse<CourseSummary>> response
            ) {
                if (!response.isSuccessful()) {
                    callback.onError("Course catalog failed to load. Status: " + response.code());
                    return;
                }

                CoursePageResponse<CourseSummary> body = response.body();
                List<CourseSummary> courses = body != null && body.content != null
                        ? body.content
                        : Collections.emptyList();

                callback.onSuccess(courses);
            }

            @Override
            public void onFailure(@NonNull Call<CoursePageResponse<CourseSummary>> call, @NonNull Throwable t) {
                callback.onError("Course catalog network error: " + safeMessage(t));
            }
        });
    }

    public void loadCourseDetail(String courseId, CourseDetailCallback callback) {
        apiService.getCourseDetail(courseId).enqueue(new Callback<CourseDetail>() {
            @Override
            public void onResponse(@NonNull Call<CourseDetail> call, @NonNull Response<CourseDetail> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Course detail failed to load. Status: " + response.code());
                    return;
                }

                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<CourseDetail> call, @NonNull Throwable t) {
                callback.onError("Course detail network error: " + safeMessage(t));
            }
        });
    }

    private String safeMessage(Throwable throwable) {
        if (throwable.getMessage() == null || throwable.getMessage().isBlank()) {
            return "Unknown error";
        }
        return throwable.getMessage();
    }
}

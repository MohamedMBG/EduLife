package com.baghdad.edulife.features.courses.data;

import androidx.annotation.NonNull;

import com.baghdad.edulife.core.network.ApiClient;
import com.baghdad.edulife.core.network.ApiService;
import com.baghdad.edulife.features.courses.model.CourseDetail;
import com.baghdad.edulife.features.courses.model.CoursePageResponse;
import com.baghdad.edulife.features.courses.model.CourseSummary;
import com.baghdad.edulife.features.courses.model.EnrolledCourse;
import com.baghdad.edulife.features.courses.model.EnrollmentResponse;
import com.baghdad.edulife.features.courses.model.EnrollRequest;
import com.baghdad.edulife.features.courses.model.LessonDetail;

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

    public interface LessonDetailCallback {
        void onSuccess(LessonDetail lessonDetail);
        void onError(String message);
    }

    public interface MarkLessonCompleteCallback {
        void onSuccess();
        void onForbidden();
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

    public void loadLessonDetail(String courseId, String lessonId, LessonDetailCallback callback) {
        apiService.getLessonDetail(courseId, lessonId).enqueue(new Callback<LessonDetail>() {
            @Override
            public void onResponse(@NonNull Call<LessonDetail> call, @NonNull Response<LessonDetail> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Lesson detail failed to load. Status: " + response.code());
                    return;
                }

                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<LessonDetail> call, @NonNull Throwable t) {
                callback.onError("Lesson detail network error: " + safeMessage(t));
            }
        });
    }

    public void markLessonComplete(String courseId, String lessonId, MarkLessonCompleteCallback callback) {
        apiService.markLessonComplete(courseId, lessonId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                    return;
                }

                if (response.code() == 403) {
                    callback.onForbidden();
                    return;
                }

                callback.onError("Could not save progress. Status: " + response.code());
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onError("Network error: " + safeMessage(t));
            }
        });
    }

    public interface EnrollCallback {
        void onSuccess(EnrollmentResponse response);
        void onError(String message);
    }

    public interface MyEnrollmentsCallback {
        void onSuccess(List<EnrolledCourse> courses);
        void onError(String message);
    }

    public interface UnenrollCallback {
        void onSuccess();
        void onError(String message);
    }

    public void enrollCourse(String courseId, EnrollCallback callback) {
        apiService.enrollCourse(new EnrollRequest(courseId)).enqueue(new Callback<EnrollmentResponse>() {
            @Override
            public void onResponse(@NonNull Call<EnrollmentResponse> call, @NonNull Response<EnrollmentResponse> response) {
                if (response.code() == 409) {
                    callback.onSuccess(response.body());
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Enrollment failed. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<EnrollmentResponse> call, @NonNull Throwable t) {
                callback.onError("Network error: " + safeMessage(t));
            }
        });
    }

    public void unenroll(String enrollmentId, UnenrollCallback callback) {
        apiService.unenroll(enrollmentId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.code() == 403) {
                    callback.onError("You don't have permission to unenroll from this course.");
                    return;
                }
                if (response.code() == 404) {
                    callback.onError("Enrollment not found.");
                    return;
                }
                if (!response.isSuccessful()) {
                    callback.onError("Unenroll failed. Status: " + response.code());
                    return;
                }
                callback.onSuccess();
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onError("Network error: " + safeMessage(t));
            }
        });
    }

    public void getMyEnrollments(MyEnrollmentsCallback callback) {
        apiService.getMyEnrollments().enqueue(new Callback<List<EnrolledCourse>>() {
            @Override
            public void onResponse(@NonNull Call<List<EnrolledCourse>> call, @NonNull Response<List<EnrolledCourse>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Failed to load enrollments. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<List<EnrolledCourse>> call, @NonNull Throwable t) {
                callback.onError("Network error: " + safeMessage(t));
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

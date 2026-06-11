package com.baghdad.edulife.features.teacher.data;

import androidx.annotation.NonNull;

import com.baghdad.edulife.core.network.ApiClient;
import com.baghdad.edulife.core.network.ApiService;
import com.baghdad.edulife.features.teacher.model.CmsCourse;
import com.baghdad.edulife.features.teacher.model.CmsLesson;
import com.baghdad.edulife.features.teacher.model.CmsSection;
import com.baghdad.edulife.features.teacher.model.CreateCourseRequest;
import com.baghdad.edulife.features.teacher.model.CreateLessonRequest;
import com.baghdad.edulife.features.teacher.model.CreateSectionRequest;

import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TeacherRepository {

    public interface CoursesCallback {
        void onSuccess(List<CmsCourse> courses);
        void onError(String message);
    }

    public interface CourseCallback {
        void onSuccess(CmsCourse course);
        void onError(String message);
    }

    public interface SectionsCallback {
        void onSuccess(List<CmsSection> sections);
        void onError(String message);
    }

    public interface SectionCallback {
        void onSuccess(CmsSection section);
        void onError(String message);
    }

    public interface LessonCallback {
        void onSuccess(CmsLesson lesson);
        void onError(String message);
    }

    public interface VoidCallback {
        void onSuccess();
        void onError(String message);
    }

    private final ApiService apiService;

    public TeacherRepository() {
        this.apiService = ApiClient.getClient().create(ApiService.class);
    }

    public void loadMyCourses(CoursesCallback callback) {
        apiService.getCmsCourses().enqueue(new Callback<List<CmsCourse>>() {
            @Override
            public void onResponse(@NonNull Call<List<CmsCourse>> call,
                                   @NonNull Response<List<CmsCourse>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Failed to load courses. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<List<CmsCourse>> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    public void createCourse(CreateCourseRequest request, CourseCallback callback) {
        apiService.createCmsCourse(request).enqueue(new Callback<CmsCourse>() {
            @Override
            public void onResponse(@NonNull Call<CmsCourse> call,
                                   @NonNull Response<CmsCourse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Failed to create course. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<CmsCourse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    public void loadSections(String courseId, SectionsCallback callback) {
        apiService.getCmsSections(courseId).enqueue(new Callback<List<CmsSection>>() {
            @Override
            public void onResponse(@NonNull Call<List<CmsSection>> call,
                                   @NonNull Response<List<CmsSection>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Failed to load sections. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<List<CmsSection>> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    public void createSection(String courseId, CreateSectionRequest request, SectionCallback callback) {
        apiService.createCmsSection(courseId, request).enqueue(new Callback<CmsSection>() {
            @Override
            public void onResponse(@NonNull Call<CmsSection> call,
                                   @NonNull Response<CmsSection> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Failed to create section. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<CmsSection> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    public void deleteSection(String courseId, String sectionId, VoidCallback callback) {
        apiService.deleteCmsSection(courseId, sectionId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call,
                                   @NonNull Response<Void> response) {
                if (!response.isSuccessful()) {
                    callback.onError("Failed to delete section. Status: " + response.code());
                    return;
                }
                callback.onSuccess();
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    public void createLesson(String sectionId, CreateLessonRequest request, LessonCallback callback) {
        apiService.createCmsLesson(sectionId, request).enqueue(new Callback<CmsLesson>() {
            @Override
            public void onResponse(@NonNull Call<CmsLesson> call,
                                   @NonNull Response<CmsLesson> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Failed to create lesson. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<CmsLesson> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    public void deleteLesson(String sectionId, String lessonId, VoidCallback callback) {
        apiService.deleteCmsLesson(sectionId, lessonId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call,
                                   @NonNull Response<Void> response) {
                if (!response.isSuccessful()) {
                    callback.onError("Failed to delete lesson. Status: " + response.code());
                    return;
                }
                callback.onSuccess();
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }
}

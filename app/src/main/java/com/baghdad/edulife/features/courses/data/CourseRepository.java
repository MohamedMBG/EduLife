package com.baghdad.edulife.features.courses.data;

import androidx.annotation.NonNull;

import com.baghdad.edulife.core.network.ApiClient;
import com.baghdad.edulife.core.network.ApiService;
import com.baghdad.edulife.features.courses.model.CourseDetail;
import com.baghdad.edulife.features.courses.model.CoursePageResponse;
import com.baghdad.edulife.features.courses.model.CourseProgressSummary;
import com.baghdad.edulife.features.courses.model.CourseSummary;
import com.baghdad.edulife.features.courses.model.EnrolledCourse;
import com.baghdad.edulife.features.courses.model.EnrollmentResponse;
import com.baghdad.edulife.features.courses.model.EnrollRequest;
import com.baghdad.edulife.features.courses.model.LessonDetail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository that mediates between the courses UI layer and the backend API for catalog browsing,
 * enrollment management, lesson details, and progress tracking.
 */
public class CourseRepository {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int ADVISOR_PAGE_SIZE = 50;

    private final ApiService apiService;

    public CourseRepository() {
        // The repository always talks through the shared Retrofit client so auth headers and
        // timeout settings stay consistent across the app.
        this.apiService = ApiClient.getClient().create(ApiService.class);
    }

    // Mirrors backend Flyway V3__seed_courses.sql so the catalog still has content
    // when the backend is unreachable (offline dev, no LAN, network errors).
    private static final List<CourseSummary> FALLBACK_COURSES = Collections.unmodifiableList(Arrays.asList(
            buildFallback("11111111-1111-1111-1111-111111111111", "math-bac-sm-algebra-foundations",
                    "Math Bac SM - Algebra Foundations",
                    "A structured algebra refresher for Moroccan Bac Sciences Math students.",
                    "BEGINNER", "fr"),
            buildFallback("22222222-2222-2222-2222-222222222222", "physics-motion-and-forces",
                    "Physics - Motion and Forces",
                    "Learn the mechanics basics needed for secondary school physics success.",
                    "INTERMEDIATE", "fr"),
            buildFallback("33333333-3333-3333-3333-333333333333", "english-communication-essentials",
                    "English Communication Essentials",
                    "Improve reading, listening, and classroom communication with practical lessons.",
                    "BEGINNER", "en"),
            buildFallback("44444444-4444-4444-4444-444444444444", "french-expression-and-writing",
                    "French Expression and Writing",
                    "Strengthen written French through structure, clarity, and revision habits.",
                    "INTERMEDIATE", "fr"),
            buildFallback("55555555-5555-5555-5555-555555555555", "digital-skills-study-productivity",
                    "Digital Skills for Study Productivity",
                    "Use practical digital habits to organize study time and course materials.",
                    "BEGINNER", "en")
    ));

    private static CourseSummary buildFallback(String id, String slug, String title,
                                               String shortDescription, String level, String lang) {
        // The fallback catalog reuses the same DTO shape as the live API so the UI can render
        // offline data without special-case mapping code.
        CourseSummary course = new CourseSummary();
        course.id = id;
        course.slug = slug;
        course.title = title;
        course.shortDescription = shortDescription;
        course.level = level;
        course.languageCode = lang;
        return course;
    }

    /**
     * Returns hardcoded seed courses filtered by category, used as a fallback when the backend
     * is unreachable during offline development.
     */
    public static List<CourseSummary> fallbackCourses(String category) {
        if (category == null || category.isBlank()) {
            return FALLBACK_COURSES;
        }
        List<CourseSummary> filtered = new ArrayList<>();
        for (CourseSummary course : FALLBACK_COURSES) {
            if (category.equalsIgnoreCase(course.level)) {
                filtered.add(course);
            }
        }
        return filtered;
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
                    // A real backend error surfaces to the UI so the learner sees a Retry CTA
                    // instead of a misleading "stale seeded list" pretending the call worked.
                    callback.onError("Catalog failed to load. Status: " + response.code());
                    return;
                }

                CoursePageResponse<CourseSummary> body = response.body();
                List<CourseSummary> courses = body != null && body.content != null
                        ? body.content
                        : Collections.emptyList();
                // Empty response is a legitimate state — no published courses match the filter.
                // The UI distinguishes empty from error via the existing catalog_empty string.
                callback.onSuccess(courses);
            }

            @Override
            public void onFailure(@NonNull Call<CoursePageResponse<CourseSummary>> call, @NonNull Throwable t) {
                callback.onError("Catalog network error: " + safeMessage(t));
            }
        });
    }

    /**
     * Loads the full unfiltered course catalog for the career advisor feature, ignoring
     * the currently selected Home filter so guidance considers all available courses.
     */
    public void loadCoursesForAdvisor(CourseCatalogCallback callback) {
        // The advisor must compare against the published catalog as a whole, not the currently
        // selected Home filter, otherwise career guidance could miss a stronger course match.
        apiService.getCourses(null, 0, ADVISOR_PAGE_SIZE).enqueue(new Callback<CoursePageResponse<CourseSummary>>() {
            @Override
            public void onResponse(
                    @NonNull Call<CoursePageResponse<CourseSummary>> call,
                    @NonNull Response<CoursePageResponse<CourseSummary>> response
            ) {
                if (!response.isSuccessful()) {
                    callback.onError("Advisor failed to load courses. Status: " + response.code());
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
                callback.onError("Advisor network error: " + safeMessage(t));
            }
        });
    }

    public void loadCourseDetail(String courseId, CourseDetailCallback callback) {
        // Course detail is a read-only screen, so any missing body is treated as a load failure
        // instead of silently constructing a partial view that could confuse the learner.
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

    public interface EnrollCallback {
        void onSuccess(EnrollmentResponse response);
        /**
         * Triggered when the backend returns 409 — learner is already enrolled. Surfacing this
         * separately lets the UI show "already enrolled" instead of pretending a fresh enrol
         * happened and lets My Courses skip the optimistic count bump.
         */
        void onAlreadyEnrolled(EnrollmentResponse response);
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

    /**
     * Enrolls the current learner in a course, distinguishing 409 (already enrolled) from
     * other errors so the UI can show a specific message instead of a generic failure.
     */
    public void enrollCourse(String courseId, EnrollCallback callback) {
        apiService.enrollCourse(new EnrollRequest(courseId)).enqueue(new Callback<EnrollmentResponse>() {
            @Override
            public void onResponse(@NonNull Call<EnrollmentResponse> call, @NonNull Response<EnrollmentResponse> response) {
                if (response.code() == 409) {
                    callback.onAlreadyEnrolled(response.body());
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

    /** Removes the learner's enrollment, handling 403 (forbidden) and 404 (not found) distinctly. */
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

    public interface LessonDetailCallback {
        void onSuccess(LessonDetail detail);
        void onError(String message);
    }

    public interface CourseProgressCallback {
        void onSuccess(CourseProgressSummary progress);
        void onError(String message);
    }

    /**
     * Reason for a mark-complete failure so the UI can pick a specific message instead of
     * stringly-typing HTTP status codes inside the fragment.
     */
    public enum MarkCompleteFailure { NOT_ENROLLED, NETWORK, OTHER }

    public interface MarkCompleteCallback {
        void onSuccess();
        void onError(MarkCompleteFailure reason);
    }

    public void loadLessonDetail(String courseId, String lessonId, LessonDetailCallback callback) {
        apiService.getLessonDetail(courseId, lessonId).enqueue(new Callback<LessonDetail>() {
            @Override
            public void onResponse(@NonNull Call<LessonDetail> call, @NonNull Response<LessonDetail> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Lesson detail failed. Status: " + response.code());
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

    /**
     * Marks a lesson as complete on the backend, mapping HTTP 403 to
     * {@link MarkCompleteFailure#NOT_ENROLLED} so the UI can show a targeted message.
     */
    public void markLessonComplete(String courseId, String lessonId, MarkCompleteCallback callback) {
        apiService.markLessonComplete(courseId, lessonId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                    return;
                }
                callback.onError(response.code() == 403
                        ? MarkCompleteFailure.NOT_ENROLLED
                        : MarkCompleteFailure.OTHER);
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onError(MarkCompleteFailure.NETWORK);
            }
        });
    }

    public void getCourseProgress(String courseId, CourseProgressCallback callback) {
        // Progress is permissioned data, so 403/404 are surfaced as explicit domain messages
        // instead of being collapsed into a generic network failure.
        apiService.getCourseProgress(courseId).enqueue(new Callback<CourseProgressSummary>() {
            @Override
            public void onResponse(@NonNull Call<CourseProgressSummary> call, @NonNull Response<CourseProgressSummary> response) {
                if (response.code() == 403) {
                    callback.onError("You must be enrolled to view progress for this course.");
                    return;
                }
                if (response.code() == 404) {
                    callback.onError("Course progress not found.");
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Course progress failed to load. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<CourseProgressSummary> call, @NonNull Throwable t) {
                callback.onError("Network error: " + safeMessage(t));
            }
        });
    }

    public void getMyEnrollments(MyEnrollmentsCallback callback) {
        // The enrolled-courses list drives the learner's personal dashboard, so an empty or
        // missing body is treated as an API problem rather than an empty state.
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
        // Retrofit failures often arrive without a useful message, so this normalizes the text
        // before it reaches the UI and keeps error handling stable.
        if (throwable.getMessage() == null || throwable.getMessage().isBlank()) {
            return "Unknown error";
        }
        return throwable.getMessage();
    }
}

package com.baghdad.edulife.core.network;

import com.baghdad.edulife.features.auth.model.AuthSyncResponse;
import com.baghdad.edulife.features.certificates.model.CertificateSummary;
import com.baghdad.edulife.features.courses.model.CourseDetail;
import com.baghdad.edulife.features.courses.model.CoursePageResponse;
import com.baghdad.edulife.features.courses.model.CourseSummary;
import com.baghdad.edulife.features.courses.model.EnrolledCourse;
import com.baghdad.edulife.features.courses.model.EnrollmentResponse;
import com.baghdad.edulife.features.courses.model.EnrollRequest;
import com.baghdad.edulife.features.courses.model.ExamResponse;
import com.baghdad.edulife.features.courses.model.ExamResultResponse;
import com.baghdad.edulife.features.courses.model.LessonDetail;
import com.baghdad.edulife.features.courses.model.SubmitExamRequest;
import com.baghdad.edulife.features.profile.model.ProfileResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Retrofit interface defining all EduLife backend API endpoints.
 *
 * The Firebase Bearer token is injected automatically by FirebaseAuthInterceptor.
 * Do not add manual Authorization headers here.
 */
public interface ApiService {

    /**
     * Syncs the authenticated Firebase user with the EduLife backend.
     * Returns the internal userId and role for local session storage.
     *
     * Endpoint: POST /api/v1/auth/sync
     * Authorization: Bearer <Firebase ID token>  (added by FirebaseAuthInterceptor)
     */
    @POST("auth/sync")
    Call<AuthSyncResponse> syncUser();

    /**
     * Loads the published course catalog from the live backend.
     * The category query currently maps to the seeded course level bucket.
     */
    @GET("courses")
    Call<CoursePageResponse<CourseSummary>> getCourses(
            @Query("category") String category,
            @Query("page") int page,
            @Query("size") int size
    );

    /**
     * Loads one course with ordered sections and lesson previews.
     */
    @GET("courses/{courseId}")
    Call<CourseDetail> getCourseDetail(@Path("courseId") String courseId);

    /**
     * Enrolls the authenticated user in a course.
     * Endpoint: POST /api/v1/enrollments
     */
    @POST("enrollments")
    Call<EnrollmentResponse> enrollCourse(@Body EnrollRequest request);

    /**
     * Soft-deletes an enrollment the caller owns.
     * Endpoint: DELETE /api/v1/enrollments/{id}
     * Returns 204 on success, 403 if not owner, 404 if not found.
     */
    @DELETE("enrollments/{id}")
    Call<Void> unenroll(@Path("id") String enrollmentId);

    /**
     * Returns the authenticated user's active enrollments with full course summary.
     * Endpoint: GET /api/v1/enrollments/me
     */
    @GET("enrollments/me")
    Call<List<EnrolledCourse>> getMyEnrollments();

    /**
     * Loads a single lesson with the content URL, body, and completion state.
     * Endpoint: GET /api/v1/courses/{courseId}/lessons/{lessonId}
     */
    @GET("courses/{courseId}/lessons/{lessonId}")
    Call<LessonDetail> getLessonDetail(
            @Path("courseId") String courseId,
            @Path("lessonId") String lessonId
    );

    /**
     * Marks a lesson as complete for the authenticated user. Idempotent.
     * Endpoint: POST /api/v1/courses/{courseId}/lessons/{lessonId}/complete
     * Returns 204 on success, 403 if not enrolled, 404 if lesson not found.
     */
    @POST("courses/{courseId}/lessons/{lessonId}/complete")
    Call<Void> markLessonComplete(
            @Path("courseId") String courseId,
            @Path("lessonId") String lessonId
    );

    @GET("profile")
    Call<ProfileResponse> getProfile();

    /**
     * Self-service account deletion (Play Store mandate).
     * Endpoint: DELETE /api/v1/account
     * Returns 204 on success. The server anonymizes the user row, deletes the Firebase account,
     * and revokes the in-flight session, so the same Bearer token will not work afterwards.
     */
    @DELETE("account")
    Call<Void> deleteAccount();

    @GET("courses/{courseId}/exam")
    Call<ExamResponse> getExam(@Path("courseId") String courseId);

    @POST("courses/{courseId}/exam/submit")
    Call<ExamResultResponse> submitExam(
            @Path("courseId") String courseId,
            @Body SubmitExamRequest request
    );

    @GET("certificates/me")
    Call<List<CertificateSummary>> getMyCertificates();
}


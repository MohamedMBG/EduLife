package com.baghdad.edulife.core.network;

import com.baghdad.edulife.features.auth.model.AuthSyncResponse;
import com.baghdad.edulife.features.certificates.model.CertificateDto;
import com.baghdad.edulife.features.courses.model.CourseDetail;
import com.baghdad.edulife.features.courses.model.CoursePageResponse;
import com.baghdad.edulife.features.courses.model.CourseSummary;
import com.baghdad.edulife.features.courses.model.EnrolledCourse;
import com.baghdad.edulife.features.courses.model.EnrollmentResponse;
import com.baghdad.edulife.features.courses.model.EnrollRequest;
import com.baghdad.edulife.features.courses.model.LessonDetail;
import com.baghdad.edulife.features.courses.model.CourseProgress;
import com.baghdad.edulife.features.exam.model.ExamDto;
import com.baghdad.edulife.features.exam.model.ExamResultDto;
import com.baghdad.edulife.features.exam.model.SubmitExamRequest;
import com.baghdad.edulife.features.profile.model.ProfileResponse;
import com.baghdad.edulife.features.profile.model.UpdateProfileRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.POST;
import retrofit2.http.PUT;
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
     * Loads the canonical lesson detail from the backend so the player does not rely on
     * navigation-passed placeholders once the real learner flow is available.
     */
    @GET("courses/{courseId}/lessons/{lessonId}")
    Call<LessonDetail> getLessonDetail(
            @Path("courseId") String courseId,
            @Path("lessonId") String lessonId
    );

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
     * Marks a lesson as complete for the authenticated user. Idempotent.
     * Endpoint: POST /api/v1/courses/{courseId}/lessons/{lessonId}/complete
     * Returns 204 on success, 403 if not enrolled, 404 if lesson not found.
     */
    @POST("courses/{courseId}/lessons/{lessonId}/complete")
    Call<Void> markLessonComplete(
            @Path("courseId") String courseId,
            @Path("lessonId") String lessonId
    );

    /**
     * Returns the authenticated learner's course progress so lesson and exam CTAs can follow
     * the real backend rules instead of optimistic placeholder state.
     */
    @GET("courses/{courseId}/progress")
    Call<CourseProgress> getCourseProgress(@Path("courseId") String courseId);

    @GET("profile")
    Call<ProfileResponse> getProfile();

    @PUT("profile")
    Call<ProfileResponse> updateProfile(@Body UpdateProfileRequest request);

    /**
     * Returns the exam for a course (questions with shuffled choices, no isCorrect field).
     * Endpoint: GET /api/v1/courses/{courseId}/exam
     * Returns 403 if not enrolled, 404 if no exam exists for the course.
     */
    @GET("courses/{courseId}/exam")
    Call<ExamDto> getExam(@Path("courseId") String courseId);

    /**
     * Submits exam answers for server-side scoring. Issues a certificate on first pass.
     * Endpoint: POST /api/v1/courses/{courseId}/exam/submit
     * Returns 403 if not enrolled, 409 if a passing attempt already exists.
     */
    @POST("courses/{courseId}/exam/submit")
    Call<ExamResultDto> submitExam(
            @Path("courseId") String courseId,
            @Body SubmitExamRequest request
    );

    /**
     * Returns all certificates earned by the authenticated user.
     * Endpoint: GET /api/v1/certificates
     */
    @GET("certificates")
    Call<List<CertificateDto>> getMyCertificates();
}


package com.baghdad.edulife.core.network;

import com.baghdad.edulife.features.auth.model.AuthSyncResponse;
import com.baghdad.edulife.features.courses.model.CourseDetail;
import com.baghdad.edulife.features.courses.model.CoursePageResponse;
import com.baghdad.edulife.features.courses.model.CourseSummary;
import com.baghdad.edulife.features.courses.model.EnrolledCourse;
import com.baghdad.edulife.features.courses.model.EnrollmentResponse;
import com.baghdad.edulife.features.courses.model.EnrollRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
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
     * Returns the authenticated user's active enrollments with course metadata.
     * Endpoint: GET /api/v1/enrollments
     */
    @GET("enrollments")
    Call<List<EnrolledCourse>> getMyEnrollments();
}


package com.baghdad.edulife.core.network;

import com.baghdad.edulife.features.admin.model.AdminPageResponse;
import com.baghdad.edulife.features.admin.model.AdminRejectRequest;
import com.baghdad.edulife.features.admin.model.AdminStats;
import com.baghdad.edulife.features.admin.model.AdminTeacherRequest;
import com.baghdad.edulife.features.teacher.model.CmsCourse;
import com.baghdad.edulife.features.teacher.model.CmsLesson;
import com.baghdad.edulife.features.teacher.model.CmsSection;
import com.baghdad.edulife.features.teacher.model.CreateCourseRequest;
import com.baghdad.edulife.features.teacher.model.CreateLessonRequest;
import com.baghdad.edulife.features.teacher.model.CreateSectionRequest;
import com.baghdad.edulife.features.auth.model.AuthSyncRequest;
import com.baghdad.edulife.features.auth.model.AuthSyncResponse;
import com.baghdad.edulife.features.certificates.model.CertificateDetail;
import com.baghdad.edulife.features.certificates.model.CertificateSummary;
import com.baghdad.edulife.features.courses.model.CourseDetail;
import com.baghdad.edulife.features.courses.model.CoursePageResponse;
import com.baghdad.edulife.features.courses.model.CourseProgressSummary;
import com.baghdad.edulife.features.courses.model.CourseSummary;
import com.baghdad.edulife.features.courses.model.EnrolledCourse;
import com.baghdad.edulife.features.courses.model.EnrollmentResponse;
import com.baghdad.edulife.features.courses.model.EnrollRequest;
import com.baghdad.edulife.features.courses.model.CourseProgressResponse;
import com.baghdad.edulife.features.courses.model.ExamResponse;
import com.baghdad.edulife.features.courses.model.ExamResultResponse;
import com.baghdad.edulife.features.courses.model.ExamStatusResponse;
import com.baghdad.edulife.features.courses.model.LessonDetail;
import com.baghdad.edulife.features.courses.model.SubmitExamRequest;
import com.baghdad.edulife.features.groupadmin.model.AddMemberRequest;
import com.baghdad.edulife.features.groupadmin.model.AttachCourseRequest;
import com.baghdad.edulife.features.groupadmin.model.CreateGroupRequest;
import com.baghdad.edulife.features.groupadmin.model.GroupDetail;
import com.baghdad.edulife.features.groupadmin.model.GroupSummary;
import com.baghdad.edulife.features.profile.model.AvatarUploadResponse;
import com.baghdad.edulife.features.profile.model.ProfileResponse;
import com.baghdad.edulife.features.profile.model.SubmitTeacherRequestBody;
import com.baghdad.edulife.features.profile.model.TeacherRequestResponse;
import com.baghdad.edulife.features.profile.model.UpdateProfileRequest;

import java.util.List;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.Part;
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
     * Sends the chosen first-time role during the initial verified auth sync.
     * The backend ignores this body for existing users and never allows ADMIN self-assignment.
     */
    @POST("auth/sync")
    Call<AuthSyncResponse> syncUser(@Body AuthSyncRequest request);

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

    /**
     * Returns aggregate progress for one enrolled course so card summaries can show
     * completed-vs-total lessons and completion percentage.
     */
    @GET("progress/courses/{courseId}")
    Call<CourseProgressSummary> getCourseProgress(@Path("courseId") String courseId);

    @GET("profile")
    Call<ProfileResponse> getProfile();

    @PUT("profile")
    Call<ProfileResponse> updateProfile(@Body UpdateProfileRequest request);

    @Multipart
    @POST("profile/avatar")
    Call<AvatarUploadResponse> uploadAvatar(@Part MultipartBody.Part file);

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

    /**
     * Checks whether the learner can start the exam before Android renders the full question UI.
     * This prevents already-passed and cooldown users from reaching a dead-end submit error.
     */
    @GET("courses/{courseId}/exam/status")
    Call<ExamStatusResponse> getExamStatus(@Path("courseId") String courseId);

    @POST("courses/{courseId}/exam/submit")
    Call<ExamResultResponse> submitExam(
            @Path("courseId") String courseId,
            @Body SubmitExamRequest request
    );

    @GET("certificates/me")
    Call<List<CertificateSummary>> getMyCertificates();

    @GET("certificates/{id}")
    Call<CertificateDetail> getCertificateById(@Path("id") String id);

    @POST("teacher-requests")
    Call<TeacherRequestResponse> submitTeacherRequest(@Body SubmitTeacherRequestBody body);

    @GET("teacher-requests/me")
    Call<TeacherRequestResponse> getMyTeacherRequest();

    // ── Admin endpoints (ADMIN role required) ─────────────────────────────

    @GET("admin/metrics")
    Call<AdminStats> getAdminStats();

    @GET("admin/teacher-requests")
    Call<AdminPageResponse<AdminTeacherRequest>> getAdminTeacherRequests(
            @Query("status") String status,
            @Query("page") int page,
            @Query("size") int size
    );

    @PUT("admin/teacher-requests/{id}/approve")
    Call<AdminTeacherRequest> approveTeacherRequest(@Path("id") String id);

    @PUT("admin/teacher-requests/{id}/reject")
    Call<AdminTeacherRequest> rejectTeacherRequest(
            @Path("id") String id,
            @Body AdminRejectRequest body
    );

    // ── CMS endpoints — TEACHER role required (token enforced by FirebaseAuthInterceptor + backend RBAC) ─

    @GET("cms/courses")
    Call<List<CmsCourse>> getCmsCourses();

    @POST("cms/courses")
    Call<CmsCourse> createCmsCourse(@Body CreateCourseRequest request);

    @PUT("cms/courses/{id}")
    Call<CmsCourse> updateCmsCourse(@Path("id") String id, @Body CreateCourseRequest request);

    @GET("cms/courses/{courseId}/sections")
    Call<List<CmsSection>> getCmsSections(@Path("courseId") String courseId);

    @POST("cms/courses/{courseId}/sections")
    Call<CmsSection> createCmsSection(@Path("courseId") String courseId, @Body CreateSectionRequest request);

    @DELETE("cms/courses/{courseId}/sections/{sectionId}")
    Call<Void> deleteCmsSection(@Path("courseId") String courseId, @Path("sectionId") String sectionId);

    @POST("cms/sections/{sectionId}/lessons")
    Call<CmsLesson> createCmsLesson(@Path("sectionId") String sectionId, @Body CreateLessonRequest request);

    @DELETE("cms/sections/{sectionId}/lessons/{lessonId}")
    Call<Void> deleteCmsLesson(@Path("sectionId") String sectionId, @Path("lessonId") String lessonId);

    /**
     * Approves (publishes) a DRAFT course. GROUP_ADMIN may only approve courses authored by a
     * teacher inside one of their groups; ADMIN may approve anything. Enforced server-side.
     * PUT /api/v1/cms/courses/{id}/publish
     */
    @PUT("cms/courses/{id}/publish")
    Call<CmsCourse> publishCmsCourse(@Path("id") String id);

    // ── Group management — GROUP_ADMIN / TEACHER / ADMIN (ownership enforced server-side) ─

    /** Groups owned by the caller (ADMIN sees all), with member/course counts. */
    @GET("groups")
    Call<List<GroupSummary>> getMyGroups();

    /** Members and attached courses of one group; owner or ADMIN only. */
    @GET("groups/{groupId}")
    Call<GroupDetail> getGroupDetail(@Path("groupId") String groupId);

    /** Creates a group owned by the caller. Body returned is ignored — callers reload the list. */
    @POST("groups")
    Call<Void> createGroup(@Body CreateGroupRequest request);

    /** Adds a member by email so admins never need internal user ids. */
    @POST("groups/{groupId}/members")
    Call<Void> addGroupMember(@Path("groupId") String groupId, @Body AddMemberRequest request);

    @DELETE("groups/{groupId}/members/{userId}")
    Call<Void> removeGroupMember(@Path("groupId") String groupId, @Path("userId") String userId);

    /** Attaches a published course to the group so its members get the cohort's curriculum. */
    @POST("groups/{groupId}/courses")
    Call<Void> attachGroupCourse(@Path("groupId") String groupId, @Body AttachCourseRequest request);
}


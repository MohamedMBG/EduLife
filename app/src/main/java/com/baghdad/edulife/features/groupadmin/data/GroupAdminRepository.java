package com.baghdad.edulife.features.groupadmin.data;

import androidx.annotation.NonNull;

import com.baghdad.edulife.core.network.ApiClient;
import com.baghdad.edulife.core.network.ApiService;
import com.baghdad.edulife.features.courses.model.CoursePageResponse;
import com.baghdad.edulife.features.courses.model.CourseSummary;
import com.baghdad.edulife.features.groupadmin.model.AddMemberRequest;
import com.baghdad.edulife.features.groupadmin.model.AttachCourseRequest;
import com.baghdad.edulife.features.groupadmin.model.CreateGroupRequest;
import com.baghdad.edulife.features.groupadmin.model.GroupDetail;
import com.baghdad.edulife.features.groupadmin.model.GroupSummary;
import com.baghdad.edulife.features.teacher.model.CmsCourse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Data layer for the group admin feature. All API calls live here so the fragments and
 * view models never touch Retrofit directly (per the project MVVM rules).
 */
public class GroupAdminRepository {

    public interface GroupsCallback {
        void onSuccess(List<GroupSummary> groups);
        void onError(String message);
    }

    public interface DetailCallback {
        void onSuccess(GroupDetail detail);
        void onError(String message);
    }

    public interface CoursesCallback {
        void onSuccess(List<CmsCourse> courses);
        void onError(String message);
    }

    public interface CatalogCallback {
        void onSuccess(List<CourseSummary> courses);
        void onError(String message);
    }

    public interface VoidCallback {
        void onSuccess();
        void onError(String message);
    }

    private final ApiService apiService;

    public GroupAdminRepository() {
        this.apiService = ApiClient.getClient().create(ApiService.class);
    }

    public void loadGroups(GroupsCallback callback) {
        apiService.getMyGroups().enqueue(new Callback<List<GroupSummary>>() {
            @Override
            public void onResponse(@NonNull Call<List<GroupSummary>> call,
                                   @NonNull Response<List<GroupSummary>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Failed to load groups. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<List<GroupSummary>> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    public void createGroup(String name, VoidCallback callback) {
        apiService.createGroup(new CreateGroupRequest(name)).enqueue(voidHandler(
                callback, "Failed to create group. Status: "));
    }

    public void loadGroupDetail(String groupId, DetailCallback callback) {
        apiService.getGroupDetail(groupId).enqueue(new Callback<GroupDetail>() {
            @Override
            public void onResponse(@NonNull Call<GroupDetail> call,
                                   @NonNull Response<GroupDetail> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Failed to load group. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<GroupDetail> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    public void addMember(String groupId, String email, VoidCallback callback) {
        apiService.addGroupMember(groupId, new AddMemberRequest(email)).enqueue(voidHandler(
                callback, "Failed to add member. Status: "));
    }

    public void removeMember(String groupId, String userId, VoidCallback callback) {
        apiService.removeGroupMember(groupId, userId).enqueue(voidHandler(
                callback, "Failed to remove member. Status: "));
    }

    public void attachCourse(String groupId, String courseId, VoidCallback callback) {
        apiService.attachGroupCourse(groupId, new AttachCourseRequest(courseId)).enqueue(voidHandler(
                callback, "Failed to assign course. Status: "));
    }

    /** Loads the CMS review queue. For GROUP_ADMIN the backend scopes this to their teachers. */
    public void loadReviewQueue(CoursesCallback callback) {
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

    public void publishCourse(String courseId, VoidCallback callback) {
        apiService.publishCmsCourse(courseId).enqueue(new Callback<CmsCourse>() {
            @Override
            public void onResponse(@NonNull Call<CmsCourse> call,
                                   @NonNull Response<CmsCourse> response) {
                if (!response.isSuccessful()) {
                    // 403 means the course's author is not in one of this admin's groups.
                    callback.onError(response.code() == 403
                            ? "You can only approve courses from teachers in your groups."
                            : "Failed to approve course. Status: " + response.code());
                    return;
                }
                callback.onSuccess();
            }

            @Override
            public void onFailure(@NonNull Call<CmsCourse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    /** Published catalog used to populate the "assign course" picker on the group detail screen. */
    public void loadPublishedCatalog(CatalogCallback callback) {
        // page 0 / size 100 is enough for the MVP catalog; the picker is a simple list.
        apiService.getCourses(null, 0, 100).enqueue(new Callback<CoursePageResponse<CourseSummary>>() {
            @Override
            public void onResponse(@NonNull Call<CoursePageResponse<CourseSummary>> call,
                                   @NonNull Response<CoursePageResponse<CourseSummary>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Failed to load catalog. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body().content);
            }

            @Override
            public void onFailure(@NonNull Call<CoursePageResponse<CourseSummary>> call,
                                  @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    /** Shared handler for endpoints that return no meaningful body (create/add/remove/attach). */
    private Callback<Void> voidHandler(VoidCallback callback, String errorPrefix) {
        return new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (!response.isSuccessful()) {
                    callback.onError(errorPrefix + response.code());
                    return;
                }
                callback.onSuccess();
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        };
    }
}

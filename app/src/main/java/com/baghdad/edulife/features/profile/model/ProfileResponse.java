package com.baghdad.edulife.features.profile.model;

import com.google.gson.annotations.SerializedName;

public class ProfileResponse {
    @SerializedName("userId") public String userId;
    @SerializedName("email") public String email;
    @SerializedName("displayName") public String displayName;
    @SerializedName("bio") public String bio;
    @SerializedName("avatarUrl") public String avatarUrl;
    @SerializedName("enrolledCourses") public int enrolledCourses;
    @SerializedName("completedLessons") public int completedLessons;
    @SerializedName("certificates") public int certificates;
}

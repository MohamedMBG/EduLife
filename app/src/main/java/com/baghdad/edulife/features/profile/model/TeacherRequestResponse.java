package com.baghdad.edulife.features.profile.model;

import com.google.gson.annotations.SerializedName;

public class TeacherRequestResponse {
    @SerializedName("id") public String id;
    @SerializedName("userId") public String userId;
    @SerializedName("userEmail") public String userEmail;
    @SerializedName("status") public String status;
    @SerializedName("motivation") public String motivation;
    @SerializedName("adminNote") public String adminNote;
    @SerializedName("requestedAt") public String requestedAt;
    @SerializedName("reviewedAt") public String reviewedAt;
}

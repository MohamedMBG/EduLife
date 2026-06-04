package com.baghdad.edulife.features.profile.model;

public class UpdateProfileRequest {
    public String displayName;
    public String bio;

    public UpdateProfileRequest(String displayName, String bio) {
        this.displayName = displayName;
        this.bio = bio;
    }
}

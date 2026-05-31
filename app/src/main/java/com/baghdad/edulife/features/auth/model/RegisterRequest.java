package com.baghdad.edulife.features.auth.model;

public class RegisterRequest {
    public static final String ROLE_LEARNER = "LEARNER";
    public static final String ROLE_TEACHER = "TEACHER";
    public static final String ROLE_GROUP_ADMIN = "GROUP_ADMIN";

    public String fullName;
    public String email;
    public String password;
    public String intendedRole;

    public RegisterRequest(String fullName, String email, String password, String intendedRole) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.intendedRole = intendedRole;
    }
}

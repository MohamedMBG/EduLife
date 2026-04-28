package com.baghdad.edulife.features.auth.model;

public class User {
    public String email;
    public boolean isEmailVerified;

    public User(String email, boolean isEmailVerified) {
        this.email = email;
        this.isEmailVerified = isEmailVerified;
    }
}
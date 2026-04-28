package com.baghdad.edulife.features.auth.model;

public class RegisterRequest {
    public String email;
    public String password;

    public RegisterRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
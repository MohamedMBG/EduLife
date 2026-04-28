package com.baghdad.edulife.features.auth.model;

public class AuthResult {
    public final boolean success;
    public final String message;
    public final boolean emailVerificationRequired;

    public AuthResult(boolean success, String message, boolean emailVerificationRequired) {
        this.success = success;
        this.message = message;
        this.emailVerificationRequired = emailVerificationRequired;
    }
}
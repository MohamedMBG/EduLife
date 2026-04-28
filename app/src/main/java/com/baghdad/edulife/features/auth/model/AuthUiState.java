package com.baghdad.edulife.features.auth.model;

public class AuthUiState {

    public final boolean loading;
    public final boolean success;
    public final boolean emailVerificationRequired;
    public final String message;

    public AuthUiState(boolean loading, boolean success, boolean emailVerificationRequired, String message) {
        this.loading = loading;
        this.success = success;
        this.emailVerificationRequired = emailVerificationRequired;
        this.message = message;
    }

    public static AuthUiState idle() {
        return new AuthUiState(false, false, false, null);
    }

    public static AuthUiState loading() {
        return new AuthUiState(true, false, false, null);
    }

    public static AuthUiState success(String message) {
        return new AuthUiState(false, true, false, message);
    }

    public static AuthUiState verificationRequired(String message) {
        return new AuthUiState(false, false, true, message);
    }

    public static AuthUiState error(String message) {
        return new AuthUiState(false, false, false, message);
    }
}
package com.baghdad.edulife.features.auth.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.baghdad.edulife.features.auth.data.AuthRepository;
import com.baghdad.edulife.features.auth.model.AuthResult;
import com.baghdad.edulife.features.auth.model.AuthUiState;

/**
 * AuthViewModel manages the authentication UI state for login and registration flows.
 *
 * Extends AndroidViewModel to access Application context, which is required to construct
 * SessionStorage (via AuthRepository) without leaking an Activity reference.
 */
public class AuthViewModel extends AndroidViewModel {

    private final AuthRepository authRepository;

    private final MutableLiveData<AuthUiState> authState =
            new MutableLiveData<>(AuthUiState.idle());

    public AuthViewModel(@NonNull Application application) {
        super(application);
        // Application context is safe here; it lives for the entire app lifetime
        this.authRepository = new AuthRepository(application.getApplicationContext());
    }

    public LiveData<AuthUiState> getAuthState() {
        return authState;
    }

    public void register(String email, String password) {
        authState.setValue(AuthUiState.loading());

        authRepository.register(email, password, result -> {
            if (result.success) {
                authState.postValue(AuthUiState.verificationRequired(result.message));
            } else {
                authState.postValue(AuthUiState.error(safeMessage(result)));
            }
        });
    }

    public void login(String email, String password) {
        authState.setValue(AuthUiState.loading());

        authRepository.login(email, password, result -> {
            if (result.success) {
                authState.postValue(AuthUiState.success("Login successful."));
                // Backend sync is best-effort: it persists userId/role when reachable
                // but never blocks or fails the login UI.
                authRepository.syncWithBackend(syncResult -> { /* no-op */ });
            } else if (result.emailVerificationRequired) {
                authState.postValue(AuthUiState.verificationRequired(result.message));
            } else {
                authState.postValue(AuthUiState.error(safeMessage(result)));
            }
        });
    }

    public void sendPasswordResetEmail(String email, AuthRepository.PasswordResetCallback callback) {
        authRepository.sendPasswordResetEmail(email, callback);
    }

    public void resetState() {
        authState.setValue(AuthUiState.idle());
    }

    /**
     * Signs the user out completely: Firebase session + local EduLife session.
     * Both are always cleared together so the two sources never become inconsistent.
     */
    public void signOut() {
        // AuthRepository.signOut() calls both FirebaseAuth.signOut() and SessionStorage.clearSession()
        authRepository.signOut();
        authState.setValue(AuthUiState.idle());
    }

    private String safeMessage(AuthResult result) {
        if (result.message == null || result.message.isBlank()) {
            return "Something went wrong. Please try again.";
        }
        return result.message;
    }
}

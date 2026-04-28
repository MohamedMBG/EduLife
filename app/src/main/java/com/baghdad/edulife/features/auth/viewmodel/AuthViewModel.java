package com.baghdad.edulife.features.auth.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.baghdad.edulife.features.auth.data.AuthRepository;
import com.baghdad.edulife.features.auth.model.AuthResult;
import com.baghdad.edulife.features.auth.model.AuthUiState;

public class AuthViewModel extends ViewModel {

    private final AuthRepository authRepository;

    private final MutableLiveData<AuthUiState> authState =
            new MutableLiveData<>(AuthUiState.idle());

    public AuthViewModel() {
        this.authRepository = new AuthRepository();
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
                authRepository.prepareBackendSyncToken(syncResult -> {
                    if (syncResult.success) {
                        authState.postValue(AuthUiState.success("Login successful."));
                    } else if (syncResult.emailVerificationRequired) {
                        authState.postValue(AuthUiState.verificationRequired(syncResult.message));
                    } else {
                        authState.postValue(AuthUiState.error(safeMessage(syncResult)));
                    }
                });
            } else if (result.emailVerificationRequired) {
                authState.postValue(AuthUiState.verificationRequired(result.message));
            } else {
                authState.postValue(AuthUiState.error(safeMessage(result)));
            }
        });
    }

    public void resetState() {
        authState.setValue(AuthUiState.idle());
    }

    public void signOut() {
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
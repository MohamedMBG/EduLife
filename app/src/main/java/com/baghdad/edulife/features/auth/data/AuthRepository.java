package com.baghdad.edulife.features.auth.data;

import com.baghdad.edulife.features.auth.model.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthRepository {

    private final FirebaseAuth firebaseAuth;

    public AuthRepository() {
        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    public void signOut() {
        firebaseAuth.signOut();
    }

    public interface AuthCallback {
        void onResult(AuthResult result);
    }

    public void register(String email, String password, AuthCallback callback) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = firebaseAuth.getCurrentUser();

                    if (user == null) {
                        callback.onResult(new AuthResult(false, "Registration failed. User not found.", false));
                        return;
                    }

                    user.sendEmailVerification()
                            .addOnSuccessListener(unused ->
                                    callback.onResult(new AuthResult(
                                            true,
                                            "Account created. Please verify your email.",
                                            true
                                    ))
                            )
                            .addOnFailureListener(e ->
                                    callback.onResult(new AuthResult(
                                            false,
                                            e.getMessage(),
                                            false
                                    ))
                            );
                })
                .addOnFailureListener(e ->
                        callback.onResult(new AuthResult(false, e.getMessage(), false))
                );
    }

    public void login(String email, String password, AuthCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = firebaseAuth.getCurrentUser();

                    if (user == null) {
                        callback.onResult(new AuthResult(false, "Login failed. User not found.", false));
                        return;
                    }

                    user.reload()
                            .addOnSuccessListener(unused -> {
                                FirebaseUser refreshedUser = firebaseAuth.getCurrentUser();

                                if (refreshedUser == null) {
                                    callback.onResult(new AuthResult(false, "Login failed. User session expired.", false));
                                    return;
                                }

                                if (!refreshedUser.isEmailVerified()) {
                                    callback.onResult(new AuthResult(
                                            false,
                                            "Please verify your email before continuing.",
                                            true
                                    ));
                                    return;
                                }

                                callback.onResult(new AuthResult(true, "Login successful.", false));
                            })
                            .addOnFailureListener(e ->
                                    callback.onResult(new AuthResult(false, e.getMessage(), false))
                            );
                })
                .addOnFailureListener(e ->
                        callback.onResult(new AuthResult(false, e.getMessage(), false))
                );
    }

    public boolean isCurrentUserEmailVerified() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        return user != null && user.isEmailVerified();
    }

    public void prepareBackendSyncToken(AuthCallback callback) {
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            callback.onResult(new AuthResult(false, "User is not authenticated.", false));
            return;
        }

        if (!user.isEmailVerified()) {
            callback.onResult(new AuthResult(false, "Email must be verified before backend sync.", true));
            return;
        }

        user.getIdToken(true)
                .addOnSuccessListener(tokenResult -> {
                    String token = tokenResult.getToken();

                    if (token == null || token.isBlank()) {
                        callback.onResult(new AuthResult(false, "Firebase ID token is missing.", false));
                        return;
                    }

                    // Next issue: call backend POST /api/v1/auth/sync with:
                    // Authorization: Bearer <token>
                    callback.onResult(new AuthResult(true, "Backend sync token ready.", false));
                })
                .addOnFailureListener(e ->
                        callback.onResult(new AuthResult(false, e.getMessage(), false))
                );
    }
}
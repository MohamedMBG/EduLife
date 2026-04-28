package com.baghdad.edulife.features.auth.data;

import com.baghdad.edulife.features.auth.model.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class AuthRepository {

    private final FirebaseAuth firebaseAuth;

    public AuthRepository() {
        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    public interface AuthCallback {
        void onResult(AuthResult result);
    }

    public void register(String email, String password, AuthCallback callback) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    if (firebaseAuth.getCurrentUser() != null) {
                        firebaseAuth.getCurrentUser().sendEmailVerification()
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
                    }
                })
                .addOnFailureListener(e ->
                        callback.onResult(new AuthResult(
                                false,
                                e.getMessage(),
                                false
                        ))
                );
    }

    public void login(String email, String password, AuthCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    if (firebaseAuth.getCurrentUser() == null) {
                        callback.onResult(new AuthResult(false, "No Firebase user found", false));
                        return;
                    }

                    firebaseAuth.getCurrentUser().reload()
                            .addOnSuccessListener(unused -> {
                                boolean verified = firebaseAuth.getCurrentUser().isEmailVerified();

                                if (!verified) {
                                    callback.onResult(new AuthResult(
                                            false,
                                            "Please verify your email before continuing.",
                                            true
                                    ));
                                    return;
                                }

                                callback.onResult(new AuthResult(
                                        true,
                                        "Login successful",
                                        false
                                ));

                                // Later: trigger backend sync here or in ViewModel
                            })
                            .addOnFailureListener(e ->
                                    callback.onResult(new AuthResult(false, e.getMessage(), false))
                            );
                })
                .addOnFailureListener(e ->
                        callback.onResult(new AuthResult(false, e.getMessage(), false))
                );
    }

}
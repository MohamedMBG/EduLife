package com.baghdad.edulife.features.auth.data;

import android.content.Context;

import com.baghdad.edulife.core.network.ApiClient;
import com.baghdad.edulife.core.network.ApiService;
import com.baghdad.edulife.core.storage.SessionStorage;
import com.baghdad.edulife.features.auth.model.AuthResult;
import com.baghdad.edulife.features.auth.model.AuthSyncResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {

    private final FirebaseAuth firebaseAuth;
    private final ApiService apiService;
    private final SessionStorage sessionStorage;

    public AuthRepository(Context context) {
        this.firebaseAuth = FirebaseAuth.getInstance();
        // Retrofit client already has FirebaseAuthInterceptor which attaches the Bearer token
        this.apiService = ApiClient.getClient().create(ApiService.class);
        this.sessionStorage = new SessionStorage(context);
    }

    public interface AuthCallback {
        void onResult(AuthResult result);
    }

    /**
     * Signs the user out of Firebase and clears the local session.
     * Both steps are always performed together to prevent stale identity from persisting.
     */
    public void signOut() {
        firebaseAuth.signOut();
        // Clear locally stored userId and role so the next login starts fresh
        sessionStorage.clearSession();
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

                                // Email verification is enforced before any backend access
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

    /**
     * Fetches a fresh Firebase ID token and calls POST /api/v1/auth/sync.
     *
     * On success:
     *   - Persists the returned userId and role via SessionStorage
     * On failure:
     *   - Clears any stale session to prevent using an outdated identity
     *
     * SECURITY: The Firebase ID token is obtained fresh here and forwarded via the
     * FirebaseAuthInterceptor on the OkHttp layer. It is never stored locally.
     */
    public void syncWithBackend(AuthCallback callback) {
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            callback.onResult(new AuthResult(false, "User is not authenticated.", false));
            return;
        }

        // Email must be verified before the backend sync is allowed
        if (!user.isEmailVerified()) {
            callback.onResult(new AuthResult(false, "Email must be verified before backend sync.", true));
            return;
        }

        // Force-refresh the token so FirebaseAuthInterceptor has a valid one ready
        user.getIdToken(true)
                .addOnSuccessListener(tokenResult -> {
                    String token = tokenResult.getToken();

                    if (token == null || token.isBlank()) {
                        callback.onResult(new AuthResult(false, "Firebase ID token is missing.", false));
                        return;
                    }

                    // Token obtained — now call the backend sync endpoint.
                    // The actual Bearer header is injected by FirebaseAuthInterceptor, not here.
                    callBackendSync(callback);
                })
                .addOnFailureListener(e ->
                        callback.onResult(new AuthResult(false, e.getMessage(), false))
                );
    }

    /**
     * Executes the Retrofit call to POST /api/v1/auth/sync and handles the response.
     * Saves the session on success; clears it on any error path.
     */
    private void callBackendSync(AuthCallback callback) {
        apiService.syncUser().enqueue(new Callback<AuthSyncResponse>() {
            @Override
            public void onResponse(Call<AuthSyncResponse> call, Response<AuthSyncResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthSyncResponse body = response.body();

                    // Validate that the backend returned both required fields
                    if (body.userId == null || body.userId.isBlank()
                            || body.role == null || body.role.isBlank()) {
                        // Clear stale session if any, then report failure
                        sessionStorage.clearSession();
                        callback.onResult(new AuthResult(
                                false,
                                "Backend sync returned incomplete data.",
                                false
                        ));
                        return;
                    }

                    // Persist userId and role; this is the only place where session is written
                    sessionStorage.saveSession(body.userId, body.role);
                    callback.onResult(new AuthResult(true, "Sync successful.", false));

                } else {
                    // Non-2xx response means sync failed; clear to prevent stale session
                    sessionStorage.clearSession();
                    callback.onResult(new AuthResult(
                            false,
                            "Backend sync failed. Status: " + response.code(),
                            false
                    ));
                }
            }

            @Override
            public void onFailure(Call<AuthSyncResponse> call, Throwable t) {
                // Network failure; clear session to prevent stale data from a previous login
                sessionStorage.clearSession();
                callback.onResult(new AuthResult(
                        false,
                        "Network error during sync: " + t.getMessage(),
                        false
                ));
            }
        });
    }
}
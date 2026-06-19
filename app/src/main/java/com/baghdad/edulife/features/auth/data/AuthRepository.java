package com.baghdad.edulife.features.auth.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.baghdad.edulife.core.network.ApiClient;
import com.baghdad.edulife.core.network.ApiService;
import com.baghdad.edulife.core.storage.SessionStorage;
import com.baghdad.edulife.features.auth.model.AuthSyncRequest;
import com.baghdad.edulife.features.auth.model.AuthResult;
import com.baghdad.edulife.features.auth.model.AuthSyncDecision;
import com.baghdad.edulife.features.auth.model.AuthSyncResponse;
import com.baghdad.edulife.features.auth.model.RegisterRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import okhttp3.HttpUrl;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.concurrent.atomic.AtomicBoolean;

public class AuthRepository {

    private static final long AUTH_SYNC_TIMEOUT_MS = 12000L;

    private final FirebaseAuth firebaseAuth;
    private final ApiService apiService;
    private final SessionStorage sessionStorage;
    private final Handler mainHandler;

    public AuthRepository(Context context) {
        this.firebaseAuth = FirebaseAuth.getInstance();
        // Retrofit client already has FirebaseAuthInterceptor which attaches the Bearer token
        this.apiService = ApiClient.getClient().create(ApiService.class);
        this.sessionStorage = new SessionStorage(context);
        this.mainHandler = new Handler(Looper.getMainLooper());
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

    public void register(RegisterRequest request, AuthCallback callback) {
        firebaseAuth.createUserWithEmailAndPassword(request.email, request.password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = firebaseAuth.getCurrentUser();

                    if (user == null) {
                        callback.onResult(new AuthResult(false, "Registration failed. User not found.", false));
                        return;
                    }

                    // Persist the selected role now because email verification usually happens before
                    // the first backend sync, often after the app is backgrounded or restarted.
                    sessionStorage.savePendingRegistrationRole(request.intendedRole);

                    // Persist the typed full name as the Firebase displayName so /auth/sync and
                    // the profile endpoint receive a real identity instead of falling back to
                    // the email local-part. The verification email is still sent regardless of
                    // whether the display-name update succeeds because verification gates login.
                    UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                            .setDisplayName(request.fullName)
                            .build();
                    user.updateProfile(profileUpdate)
                            .addOnCompleteListener(profileTask -> user.sendEmailVerification()
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
                                    ));
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

                    callback.onResult(new AuthResult(true, "Login successful.", false));
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
            failBackendSync(callback, "User is not authenticated.");
            return;
        }

        // Force-refresh the token so FirebaseAuthInterceptor has a valid one ready
        user.getIdToken(true)
                .addOnSuccessListener(tokenResult -> {
                    String token = tokenResult.getToken();

                    if (token == null || token.isBlank()) {
                        failBackendSync(callback, "Firebase ID token is missing.");
                        return;
                    }

                    // Reuse the same freshly forced token for /auth/sync. This removes a second
                    // Firebase token fetch inside OkHttp that could fail separately from the
                    // backend call and show a misleading "cannot reach server" error.
                    callBackendSync(callback, token);
                })
                .addOnFailureListener(e ->
                        failBackendSync(callback, e.getMessage())
                );
    }

    /**
     * Executes the Retrofit call to POST /api/v1/auth/sync and handles the response.
     * Saves the session on success; clears it on any error path.
     */
    private void callBackendSync(AuthCallback callback, String firebaseIdToken) {
        String pendingRole = sessionStorage.getPendingRegistrationRole();
        String authorizationHeader = "Bearer " + firebaseIdToken;
        Call<AuthSyncResponse> syncCall = pendingRole == null || pendingRole.isBlank()
                ? apiService.syncUser(authorizationHeader)
                : apiService.syncUser(authorizationHeader, new AuthSyncRequest(pendingRole));
        AtomicBoolean callbackDelivered = new AtomicBoolean(false);
        Runnable timeoutRunnable = () -> {
            if (!callbackDelivered.compareAndSet(false, true)) {
                return;
            }

            // Cancel the in-flight sync so it does not keep a socket open after the deadline.
            // The local EduLife identity is cleared separately so launch routing cannot reuse a
            // stale role after backend identity sync failed.
            syncCall.cancel();
            failBackendSync(
                    callback,
                    "Backend sync timed out while reaching " + describeConfiguredApiTarget() + "."
            );
        };

        mainHandler.postDelayed(timeoutRunnable, AUTH_SYNC_TIMEOUT_MS);

        syncCall.enqueue(new Callback<AuthSyncResponse>() {
            @Override
            public void onResponse(Call<AuthSyncResponse> call, Response<AuthSyncResponse> response) {
                if (!callbackDelivered.compareAndSet(false, true)) {
                    return;
                }

                mainHandler.removeCallbacks(timeoutRunnable);

                // Fail-closed: the session is only persisted when sync returns a complete
                // identity. The decision logic lives in AuthSyncDecision so it can be unit-tested.
                AuthSyncDecision decision = AuthSyncDecision.fromSyncResponse(
                        response.isSuccessful(), response.code(), response.body());

                if (decision.authenticated) {
                    // Persist userId and role; this is the only place where session is written
                    sessionStorage.saveSession(decision.userId, decision.role);
                    // The backend only honors intendedRole on first sync, so the pending copy is
                    // no longer needed after any successful sync response.
                    sessionStorage.clearPendingRegistrationRole();
                    callback.onResult(new AuthResult(true, "Sync successful.", false));
                } else {
                    failBackendSync(callback, decision.message);
                }
            }

            @Override
            public void onFailure(Call<AuthSyncResponse> call, Throwable t) {
                if (!callbackDelivered.compareAndSet(false, true)) {
                    return;
                }

                mainHandler.removeCallbacks(timeoutRunnable);

                // Firebase may still be signed in, but EduLife screens must not trust a previously
                // stored backend userId/role after the authoritative sync failed.
                failBackendSync(
                        callback,
                        "Network error during sync to " + describeConfiguredApiTarget() + ": " + readableSyncFailure(t)
                );
            }
        });
    }

    private void failBackendSync(AuthCallback callback, String message) {
        // Clear only the synced backend identity. Firebase auth remains so the UI can show the
        // sync error, but MainActivity cannot route from a stale local role on next launch.
        sessionStorage.clearAuthenticatedSession();
        String readableMessage = message == null || message.isBlank()
                ? "Backend identity sync failed."
                : message;
        callback.onResult(new AuthResult(false, readableMessage, false));
    }

    private String readableSyncFailure(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank()) {
            return "Unknown network failure.";
        }

        String message = throwable.getMessage();
        if (message.contains("Canceled")) {
            return "The request was canceled after waiting too long for the backend.";
        }
        return message;
    }

    /**
     * Uses the configured base URL host instead of a generic "server" label so wrong-target APKs
     * are obvious immediately on a physical device.
     */
    private String describeConfiguredApiTarget() {
        String baseUrl = ApiClient.getBaseUrl();
        HttpUrl parsedUrl = HttpUrl.parse(baseUrl);
        if (parsedUrl == null || parsedUrl.host() == null || parsedUrl.host().isBlank()) {
            return baseUrl;
        }
        return parsedUrl.host();
    }
}

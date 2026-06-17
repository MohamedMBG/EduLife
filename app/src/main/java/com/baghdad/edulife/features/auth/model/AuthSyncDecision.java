package com.baghdad.edulife.features.auth.model;

/**
 * Pure, framework-free decision logic for the fail-closed authentication contract.
 *
 * The product rule (see AGENTS.md / CLAUDE.md backend security rules) is:
 * a successful Firebase sign-in is NOT enough to consider the user authenticated.
 * The app must complete POST /api/v1/auth/sync — receiving a valid internal userId and role —
 * before it posts an authenticated success state or navigates the user forward.
 *
 * This class isolates that rule so it can be unit-tested on the host JVM (no Android, no
 * Firebase, no network) and reused by both:
 *   - {@code AuthRepository.callBackendSync} to decide whether the session may be persisted, and
 *   - {@code AuthViewModel} to decide whether to surface success or an error to the UI.
 */
public final class AuthSyncDecision {

    /** True only when backend sync succeeded with a complete identity. */
    public final boolean authenticated;

    /** Internal EduLife userId; non-null only when {@link #authenticated} is true. */
    public final String userId;

    /** Assigned role; non-null only when {@link #authenticated} is true. */
    public final String role;

    /** Human-readable message describing the outcome (success note or failure reason). */
    public final String message;

    private AuthSyncDecision(boolean authenticated, String userId, String role, String message) {
        this.authenticated = authenticated;
        this.userId = userId;
        this.role = role;
        this.message = message;
    }

    /**
     * Evaluates the raw /auth/sync HTTP response under the fail-closed rule.
     *
     * A session is only authenticated when the call was HTTP-successful, a body was returned,
     * and both userId and role are present. Any other case is a failure and the caller must
     * NOT persist a session or navigate forward.
     *
     * @param httpSuccessful Retrofit {@code response.isSuccessful()}
     * @param statusCode     Retrofit {@code response.code()} (used to build the failure message)
     * @param body           the parsed sync response (may be null)
     */
    public static AuthSyncDecision fromSyncResponse(boolean httpSuccessful, int statusCode, AuthSyncResponse body) {
        if (!httpSuccessful || body == null) {
            return failure("Backend sync failed. Status: " + statusCode);
        }
        if (isBlank(body.userId) || isBlank(body.role)) {
            return failure("Backend sync returned incomplete data.");
        }
        return new AuthSyncDecision(true, body.userId, body.role, "Sync successful.");
    }

    /**
     * Fail-closed verdict used by the UI layer: a login/registration flow may only treat the
     * user as authenticated when the preceding backend sync produced a successful {@link AuthResult}.
     * A null result (sync never delivered) is treated as not authenticated.
     */
    public static boolean isAuthenticated(AuthResult syncResult) {
        return syncResult != null && syncResult.success;
    }

    private static AuthSyncDecision failure(String message) {
        return new AuthSyncDecision(false, null, null, message);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

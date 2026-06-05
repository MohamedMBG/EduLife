package com.baghdad.edulife.core.storage;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SessionStorage is the single source of truth for the locally persisted EduLife session identity.
 *
 * It stores only:
 *   - userId  : the internal UUID returned by /api/v1/auth/sync
 *   - role    : the user's role (e.g. LEARNER, TEACHER, GROUP_ADMIN)
 *   - pending_registration_role : the selected role waiting for the first verified /auth/sync
 *
 * SECURITY: This class intentionally never stores Firebase ID tokens, refresh tokens, or passwords.
 * Tokens are always fetched fresh from Firebase at request time via FirebaseAuthInterceptor.
 *
 * Usage pattern:
 *   - Write: after /api/v1/auth/sync succeeds
 *   - Clear: on logout or sync failure
 *   - Read:  by any feature that needs the internal identity
 */
public class SessionStorage {

    private static final String PREFS_NAME = "edulife_session";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_ROLE    = "role";
    private static final String KEY_PENDING_REGISTRATION_ROLE = "pending_registration_role";

    private final SharedPreferences prefs;

    public SessionStorage(Context context) {
        // Use application context to avoid Activity memory leaks
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Persists the internal EduLife identity returned by /api/v1/auth/sync.
     * Must only be called after a successful sync response.
     *
     * Uses commit() instead of apply() so a crash between sync success and the next process
     * launch cannot resurrect a half-written session. Sync callers already run on a background
     * thread so blocking on the encrypted disk write is acceptable here.
     *
     * @param userId internal UUID assigned by the backend
     * @param role   user role string (e.g. "LEARNER")
     */
    public void saveSession(String userId, String role) {
        prefs.edit()
                .putString(KEY_USER_ID, userId)
                .putString(KEY_ROLE, role)
                .commit();
    }

    /**
     * Returns the internally stored EduLife user ID.
     * Returns null if no session has been saved yet.
     */
    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    /**
     * Returns the stored user role.
     * Returns null if no session has been saved yet.
     */
    public String getRole() {
        return prefs.getString(KEY_ROLE, null);
    }

    /**
     * Persists the selected registration role until the first verified backend sync consumes it.
     * This survives app restarts so email verification can happen outside the app without losing intent.
     */
    public void savePendingRegistrationRole(String role) {
        prefs.edit().putString(KEY_PENDING_REGISTRATION_ROLE, role).apply();
    }

    public String getPendingRegistrationRole() {
        return prefs.getString(KEY_PENDING_REGISTRATION_ROLE, null);
    }

    public void clearPendingRegistrationRole() {
        prefs.edit().remove(KEY_PENDING_REGISTRATION_ROLE).apply();
    }

    /**
     * Returns true if a session has been persisted (i.e. sync has succeeded before).
     * This does NOT validate that the session is still active or the Firebase token is valid.
     */
    public boolean hasSession() {
        return getUserId() != null && getRole() != null;
    }

    /**
     * Clears all stored session data.
     * Must be called on logout and on /api/v1/auth/sync failure
     * to prevent stale identity from leaking into future sessions.
     *
     * Uses commit() so a crash immediately after sign-out cannot leave a stale userId/role on
     * disk and let a different user inherit it on the next launch. Logout flows already block
     * the user behind a progress indicator, so blocking on disk is the right trade-off.
     */
    public void clearSession() {
        prefs.edit().clear().commit();
    }
}

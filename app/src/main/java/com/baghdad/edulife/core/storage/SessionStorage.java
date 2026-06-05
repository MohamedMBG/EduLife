package com.baghdad.edulife.core.storage;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

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
 * All values are persisted via androidx.security EncryptedSharedPreferences so the on-disk file
 * is encrypted at rest with a key bound to the Android Keystore (AES256-GCM master key).
 *
 * Usage pattern:
 *   - Write: after /api/v1/auth/sync succeeds
 *   - Clear: on logout or sync failure
 *   - Read:  by any feature that needs the internal identity
 */
public class SessionStorage {

    // New filename so the migration from the legacy plain-text "edulife_session" file does not
    // clash with EncryptedSharedPreferences, which expects an encrypted payload format.
    // Existing users transparently re-run /auth/sync on next launch; Firebase auth state survives.
    private static final String PREFS_NAME = "edulife_session_secure";
    private static final String LEGACY_PREFS_NAME = "edulife_session";

    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_ROLE    = "role";
    private static final String KEY_PENDING_REGISTRATION_ROLE = "pending_registration_role";

    private final SharedPreferences prefs;

    public SessionStorage(Context context) {
        // Use application context to avoid Activity memory leaks across the prefs lifetime.
        Context appContext = context.getApplicationContext();
        this.prefs = openEncryptedPrefs(appContext);

        // Delete the legacy unencrypted prefs file if it survived the upgrade. Its values
        // are not migrated: re-running /auth/sync is cheaper than maintaining a plaintext shim.
        appContext.deleteSharedPreferences(LEGACY_PREFS_NAME);
    }

    private static SharedPreferences openEncryptedPrefs(Context appContext) {
        try {
            MasterKey masterKey = new MasterKey.Builder(appContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            return EncryptedSharedPreferences.create(
                    appContext,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            // Keystore can become corrupt after a device-level event (factory reset of credentials,
            // strongbox failure). Drop the encrypted file and recreate so the app does not crash on
            // launch; user simply re-syncs.
            appContext.deleteSharedPreferences(PREFS_NAME);
            try {
                MasterKey masterKey = new MasterKey.Builder(appContext)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build();
                return EncryptedSharedPreferences.create(
                        appContext,
                        PREFS_NAME,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                );
            } catch (GeneralSecurityException | IOException retryFailure) {
                throw new IllegalStateException("Unable to initialise encrypted session storage", retryFailure);
            }
        }
    }

    /**
     * Persists the internal EduLife identity returned by /api/v1/auth/sync.
     * Must only be called after a successful sync response.
     *
     * @param userId internal UUID assigned by the backend
     * @param role   user role string (e.g. "LEARNER")
     */
    public void saveSession(String userId, String role) {
        prefs.edit()
                .putString(KEY_USER_ID, userId)
                .putString(KEY_ROLE, role)
                .apply();
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
     */
    public void clearSession() {
        prefs.edit().clear().apply();
    }
}

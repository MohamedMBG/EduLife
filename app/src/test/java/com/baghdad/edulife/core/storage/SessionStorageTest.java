package com.baghdad.edulife.core.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Host-JVM tests for SessionStorage persistence semantics, run against an in-memory
 * {@link FakeSharedPreferences} via the package-private test seam. These cover the rules that
 * matter for the fail-closed session model: identity is written and read back intact, cleared
 * completely on logout, and a session is only considered present when BOTH userId and role exist.
 */
public class SessionStorageTest {

    private FakeSharedPreferences prefs;
    private SessionStorage storage;

    @Before
    public void setUp() {
        prefs = new FakeSharedPreferences();
        storage = new SessionStorage(prefs);
    }

    @Test
    public void freshStorage_hasNoSession() {
        assertNull(storage.getUserId());
        assertNull(storage.getRole());
        assertFalse(storage.hasSession());
    }

    @Test
    public void saveSession_persistsUserIdAndRole() {
        storage.saveSession("user-123", "LEARNER");

        assertEquals("user-123", storage.getUserId());
        assertEquals("LEARNER", storage.getRole());
        assertTrue(storage.hasSession());
    }

    @Test
    public void saveSession_rolePersistsExactlyAsGiven() {
        storage.saveSession("user-9", "GROUP_ADMIN");
        assertEquals("GROUP_ADMIN", storage.getRole());

        // Re-saving overwrites with the new identity rather than merging.
        storage.saveSession("user-9", "TEACHER");
        assertEquals("TEACHER", storage.getRole());
    }

    @Test
    public void clearSession_removesEverything() {
        storage.saveSession("user-123", "LEARNER");
        storage.clearSession();

        assertNull(storage.getUserId());
        assertNull(storage.getRole());
        assertFalse(storage.hasSession());
    }

    @Test
    public void hasSession_requiresBothUserIdAndRole() {
        // Only userId present (e.g. a hypothetical partial write) is NOT a valid session.
        prefs.edit().putString("user_id", "user-123").apply();
        assertFalse(storage.hasSession());

        // Only role present is also invalid.
        prefs.edit().clear().putString("role", "LEARNER").apply();
        assertFalse(storage.hasSession());

        // Both present -> valid.
        prefs.edit().clear()
                .putString("user_id", "user-123")
                .putString("role", "LEARNER")
                .apply();
        assertTrue(storage.hasSession());
    }

    @Test
    public void saveSession_writesBothFieldsTogether_noPartialSession() {
        // SessionStorage exposes no API that writes userId or role in isolation, so a session
        // can never become half-written. After saveSession both are present; there is no path
        // that leaves exactly one set.
        storage.saveSession("user-123", "LEARNER");
        assertTrue(storage.hasSession());
        assertEquals("user-123", storage.getUserId());
        assertEquals("LEARNER", storage.getRole());
    }

    @Test
    public void pendingRegistrationRole_savedReadAndCleared() {
        assertNull(storage.getPendingRegistrationRole());

        storage.savePendingRegistrationRole("TEACHER");
        assertEquals("TEACHER", storage.getPendingRegistrationRole());

        storage.clearPendingRegistrationRole();
        assertNull(storage.getPendingRegistrationRole());
    }

    @Test
    public void clearSession_alsoDropsPendingRegistrationRole() {
        storage.savePendingRegistrationRole("TEACHER");
        storage.saveSession("user-123", "LEARNER");

        storage.clearSession();

        // clear() wipes the whole prefs file, including any pending role left over from sign-up.
        assertNull(storage.getPendingRegistrationRole());
        assertFalse(storage.hasSession());
    }

    @Test
    public void clearingPendingRole_keepsAnActiveSessionIntact() {
        storage.saveSession("user-123", "LEARNER");
        storage.savePendingRegistrationRole("TEACHER");

        storage.clearPendingRegistrationRole();

        // Consuming the pending role after a successful sync must not disturb the live session.
        assertTrue(storage.hasSession());
        assertEquals("user-123", storage.getUserId());
        assertEquals("LEARNER", storage.getRole());
    }

    @Test
    public void clearAuthenticatedSession_dropsStaleIdentityButKeepsPendingRegistrationRole() {
        storage.saveSession("old-admin", "ADMIN");
        storage.savePendingRegistrationRole("TEACHER");

        storage.clearAuthenticatedSession();

        // Backend sync failure must remove stale routing data without losing first-sync intent.
        assertNull(storage.getUserId());
        assertNull(storage.getRole());
        assertFalse(storage.hasSession());
        assertEquals("TEACHER", storage.getPendingRegistrationRole());
    }
}

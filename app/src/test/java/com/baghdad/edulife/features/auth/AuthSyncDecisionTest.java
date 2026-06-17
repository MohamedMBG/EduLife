package com.baghdad.edulife.features.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.baghdad.edulife.features.auth.model.AuthResult;
import com.baghdad.edulife.features.auth.model.AuthSyncDecision;
import com.baghdad.edulife.features.auth.model.AuthSyncResponse;

import org.junit.Test;

/**
 * Host-JVM tests for the fail-closed authentication contract.
 *
 * Product rule: a successful Firebase sign-in is NOT enough. The app must complete
 * POST /api/v1/auth/sync with a valid internal userId + role before treating the user as
 * authenticated or navigating forward. AuthRepository.callBackendSync and AuthViewModel.login
 * both delegate to AuthSyncDecision, so these tests assert the real production decision path.
 */
public class AuthSyncDecisionTest {

    private static AuthSyncResponse response(String userId, String role) {
        AuthSyncResponse body = new AuthSyncResponse();
        body.userId = userId;
        body.role = role;
        return body;
    }

    // ── fromSyncResponse: the gate that decides whether the session may be persisted ──

    @Test
    public void syncSuccessWithCompleteIdentity_isAuthenticatedAndCarriesIdentity() {
        AuthSyncDecision decision =
                AuthSyncDecision.fromSyncResponse(true, 200, response("user-123", "LEARNER"));

        assertTrue(decision.authenticated);
        assertEquals("user-123", decision.userId);
        assertEquals("LEARNER", decision.role);
        assertEquals("Sync successful.", decision.message);
    }

    @Test
    public void syncHttpFailure_isNotAuthenticated_andCarriesStatusCode() {
        AuthSyncDecision decision =
                AuthSyncDecision.fromSyncResponse(false, 500, null);

        assertFalse(decision.authenticated);
        assertNull(decision.userId);
        assertNull(decision.role);
        assertEquals("Backend sync failed. Status: 500", decision.message);
    }

    @Test
    public void syncSuccessfulButNullBody_isNotAuthenticated() {
        AuthSyncDecision decision =
                AuthSyncDecision.fromSyncResponse(true, 200, null);

        assertFalse(decision.authenticated);
        assertEquals("Backend sync failed. Status: 200", decision.message);
    }

    @Test
    public void syncResponseMissingUserId_isNotAuthenticated() {
        AuthSyncDecision decision =
                AuthSyncDecision.fromSyncResponse(true, 200, response(null, "LEARNER"));

        assertFalse(decision.authenticated);
        assertNull(decision.userId);
        assertEquals("Backend sync returned incomplete data.", decision.message);
    }

    @Test
    public void syncResponseBlankUserId_isNotAuthenticated() {
        AuthSyncDecision decision =
                AuthSyncDecision.fromSyncResponse(true, 200, response("   ", "LEARNER"));

        assertFalse(decision.authenticated);
        assertEquals("Backend sync returned incomplete data.", decision.message);
    }

    @Test
    public void syncResponseMissingRole_isNotAuthenticated() {
        AuthSyncDecision decision =
                AuthSyncDecision.fromSyncResponse(true, 200, response("user-123", null));

        assertFalse(decision.authenticated);
        assertNull(decision.role);
        assertEquals("Backend sync returned incomplete data.", decision.message);
    }

    @Test
    public void syncResponseBlankRole_isNotAuthenticated() {
        AuthSyncDecision decision =
                AuthSyncDecision.fromSyncResponse(true, 200, response("user-123", ""));

        assertFalse(decision.authenticated);
        assertEquals("Backend sync returned incomplete data.", decision.message);
    }

    // ── isAuthenticated: the UI-layer fail-closed verdict used by AuthViewModel ──

    @Test
    public void firebaseLoginThenSyncSuccess_isAuthenticated() {
        // Case 1: Firebase login success + backend sync success => login success.
        AuthResult syncSuccess = new AuthResult(true, "Sync successful.", false);
        assertTrue(AuthSyncDecision.isAuthenticated(syncSuccess));
    }

    @Test
    public void firebaseLoginThenSyncFailure_isNotAuthenticated() {
        // Case 2: Firebase login success + backend sync failure => login error, no success.
        AuthResult syncFailed = new AuthResult(false, "Backend sync failed. Status: 503", false);
        assertFalse(AuthSyncDecision.isAuthenticated(syncFailed));
    }

    @Test
    public void firebaseLoginThenSyncNetworkError_isNotAuthenticated() {
        // Case 3: Firebase login success + backend sync network error => login error.
        AuthResult networkError =
                new AuthResult(false, "Network error during sync: Unable to resolve host", false);
        assertFalse(AuthSyncDecision.isAuthenticated(networkError));
    }

    @Test
    public void nullSyncResult_isNotAuthenticated() {
        // Defensive: if the sync callback never produced a result, stay locked out.
        assertFalse(AuthSyncDecision.isAuthenticated(null));
    }

    @Test
    public void registrationSyncFailure_doesNotGrantAuthentication() {
        // Case 5: a failed sync after registration must never be treated as authenticated,
        // so the dashboard/home flow cannot be reached on a sync failure.
        AuthResult syncFailed = new AuthResult(false, "Backend sync returned incomplete data.", false);
        assertFalse(AuthSyncDecision.isAuthenticated(syncFailed));
    }
}

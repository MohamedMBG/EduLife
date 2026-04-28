package com.baghdad.edulife.features.auth.model;

/**
 * Represents the response body from POST /api/v1/auth/sync.
 *
 * The backend returns:
 *   - userId : internal EduLife UUID (never the Firebase UID)
 *   - role   : assigned role string (e.g. "STUDENT", "TEACHER", "ADMIN")
 *
 * SECURITY: This model intentionally contains no Firebase tokens or credentials.
 * Field names must match the JSON keys returned by the backend exactly.
 */
public class AuthSyncResponse {

    /** Internal UUID assigned by the EduLife backend. Never the Firebase UID. */
    public String userId;

    /** Role assigned to this user on the backend (e.g. "STUDENT"). */
    public String role;
}

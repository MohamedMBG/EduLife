package com.baghdad.edulife.features.groupadmin.model;

/**
 * Request body for POST /api/v1/groups/{groupId}/members.
 * Android adds members by email so the admin never needs internal user ids;
 * the backend resolves the email to a user and validates the one-of rule.
 */
public class AddMemberRequest {
    public final String email;

    public AddMemberRequest(String email) {
        this.email = email;
    }
}

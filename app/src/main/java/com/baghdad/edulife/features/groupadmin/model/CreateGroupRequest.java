package com.baghdad.edulife.features.groupadmin.model;

/** Request body for POST /api/v1/groups. */
public class CreateGroupRequest {
    public final String name;

    public CreateGroupRequest(String name) {
        this.name = name;
    }
}

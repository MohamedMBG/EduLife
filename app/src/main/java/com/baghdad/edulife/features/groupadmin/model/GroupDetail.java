package com.baghdad.edulife.features.groupadmin.model;

import java.util.List;

/** Full group view: members and attached courses. Mirrors backend GroupDetailDto. */
public class GroupDetail {
    public String id;
    public String name;
    public String createdAt;
    public List<GroupMember> members;
    public List<GroupCourse> courses;
}

package com.edulife.groups.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Composite primary key for {@link GroupCourse} (groupId + courseId). */
public class GroupCourseId implements Serializable {

    private UUID groupId;
    private UUID courseId;

    public GroupCourseId() {}

    public GroupCourseId(UUID groupId, UUID courseId) {
        this.groupId = groupId;
        this.courseId = courseId;
    }

    public UUID getGroupId() { return groupId; }
    public UUID getCourseId() { return courseId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GroupCourseId that)) return false;
        return Objects.equals(groupId, that.groupId) && Objects.equals(courseId, that.courseId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, courseId);
    }
}

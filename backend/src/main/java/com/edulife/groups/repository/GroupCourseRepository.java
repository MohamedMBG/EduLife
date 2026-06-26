package com.edulife.groups.repository;

import com.edulife.groups.entity.GroupCourse;
import com.edulife.groups.entity.GroupCourseId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link GroupCourse} entities (group-to-course attachments). */
public interface GroupCourseRepository extends JpaRepository<GroupCourse, GroupCourseId> {

    boolean existsByGroupIdAndCourseId(UUID groupId, UUID courseId);

    List<GroupCourse> findAllByGroupId(UUID groupId);

    long countByGroupId(UUID groupId);
}

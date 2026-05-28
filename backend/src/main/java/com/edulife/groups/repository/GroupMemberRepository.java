package com.edulife.groups.repository;

import com.edulife.groups.entity.GroupMember;
import com.edulife.groups.entity.GroupMemberId;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMemberId> {

    boolean existsByGroupIdAndUserId(UUID groupId, UUID userId);

    long deleteByGroupIdAndUserId(UUID groupId, UUID userId);
}

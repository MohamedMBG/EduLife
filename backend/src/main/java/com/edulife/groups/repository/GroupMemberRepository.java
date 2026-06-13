package com.edulife.groups.repository;

import com.edulife.groups.entity.GroupMember;
import com.edulife.groups.entity.GroupMemberId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMemberId> {

    boolean existsByGroupIdAndUserId(UUID groupId, UUID userId);

    long deleteByGroupIdAndUserId(UUID groupId, UUID userId);

    List<GroupMember> findAllByGroupId(UUID groupId);

    List<GroupMember> findAllByUserId(UUID userId);

    long countByGroupId(UUID groupId);

    // "Managed by" = member of any group created by the given group admin.
    @Query(value = """
            SELECT EXISTS(
                SELECT 1 FROM group_members gm
                JOIN groups g ON gm.group_id = g.id
                WHERE g.created_by = :managerId AND gm.user_id = :userId
            )
            """, nativeQuery = true)
    boolean existsMemberManagedBy(@Param("managerId") UUID managerId, @Param("userId") UUID userId);

    @Query(value = """
            SELECT DISTINCT gm.user_id FROM group_members gm
            JOIN groups g ON gm.group_id = g.id
            WHERE g.created_by = :managerId
            """, nativeQuery = true)
    List<UUID> findMemberUserIdsManagedBy(@Param("managerId") UUID managerId);
}

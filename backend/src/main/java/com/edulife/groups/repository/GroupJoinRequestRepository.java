package com.edulife.groups.repository;

import com.edulife.groups.entity.GroupJoinRequest;
import com.edulife.groups.model.GroupJoinRequestStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link GroupJoinRequest} entities with status and group filtering. */
public interface GroupJoinRequestRepository extends JpaRepository<GroupJoinRequest, UUID> {

    boolean existsByGroupIdAndRequesterUserIdAndStatus(
            UUID groupId,
            UUID requesterUserId,
            GroupJoinRequestStatus status
    );

    Optional<GroupJoinRequest> findByIdAndGroupId(UUID id, UUID groupId);

    List<GroupJoinRequest> findAllByRequesterUserIdOrderByRequestedAtDesc(UUID requesterUserId);

    List<GroupJoinRequest> findAllByGroupIdOrderByRequestedAtDesc(UUID groupId);

    List<GroupJoinRequest> findAllByGroupIdAndStatusOrderByRequestedAtDesc(
            UUID groupId,
            GroupJoinRequestStatus status
    );
}

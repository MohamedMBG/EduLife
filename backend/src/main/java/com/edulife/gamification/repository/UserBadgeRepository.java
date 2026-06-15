package com.edulife.gamification.repository;

import com.edulife.gamification.entity.UserBadge;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserBadgeRepository extends JpaRepository<UserBadge, UserBadge.Key> {

    List<UserBadge> findAllByIdUserIdOrderByUnlockedAtAsc(UUID userId);

    @Query("SELECT b.id.badgeId FROM UserBadge b WHERE b.id.userId = :userId")
    Set<String> findBadgeIdsByUserId(@Param("userId") UUID userId);

    boolean existsByIdUserIdAndIdBadgeId(UUID userId, String badgeId);
}

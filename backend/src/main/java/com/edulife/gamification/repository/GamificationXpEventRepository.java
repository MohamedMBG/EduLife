package com.edulife.gamification.repository;

import com.edulife.gamification.entity.GamificationXpEvent;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for the append-only XP event log, supporting dedup checks and event counting.
 */
public interface GamificationXpEventRepository extends JpaRepository<GamificationXpEvent, UUID> {

    boolean existsByDedupKey(String dedupKey);

    long countByUserIdAndEventTypeAndCreatedAtAfter(
            UUID userId,
            com.edulife.gamification.model.XpEventType eventType,
            Instant after
    );

    long countByUserIdAndEventType(UUID userId, com.edulife.gamification.model.XpEventType eventType);
}

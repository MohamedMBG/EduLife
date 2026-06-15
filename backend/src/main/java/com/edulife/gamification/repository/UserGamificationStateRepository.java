package com.edulife.gamification.repository;

import com.edulife.gamification.entity.UserGamificationState;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserGamificationStateRepository extends JpaRepository<UserGamificationState, UUID> {

    List<UserGamificationState> findAllByOrderByTotalXpDescUpdatedAtAsc(Pageable pageable);
}

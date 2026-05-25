package com.edulife.progress.repository;

import com.edulife.progress.entity.LessonProgress;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, UUID> {

    boolean existsByUserIdAndLessonId(UUID userId, UUID lessonId);

    Optional<LessonProgress> findByUserIdAndLessonId(UUID userId, UUID lessonId);

    long countByUserIdAndCourseId(UUID userId, UUID courseId);

    long countByUserId(UUID userId);

    @Query("SELECT lp.lessonId FROM LessonProgress lp WHERE lp.userId = :userId AND lp.courseId = :courseId")
    Set<UUID> findCompletedLessonIdsByUserIdAndCourseId(@Param("userId") UUID userId, @Param("courseId") UUID courseId);

    List<LessonProgress> findAllByUserIdAndCourseId(UUID userId, UUID courseId);
}

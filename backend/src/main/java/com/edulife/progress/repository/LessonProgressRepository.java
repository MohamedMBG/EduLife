package com.edulife.progress.repository;

import com.edulife.progress.entity.LessonProgress;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, UUID> {

    boolean existsByUserIdAndLessonId(UUID userId, UUID lessonId);

    Optional<LessonProgress> findByUserIdAndLessonId(UUID userId, UUID lessonId);

    long countByUserIdAndCourseId(UUID userId, UUID courseId);

    long countByUserId(UUID userId);

    List<LessonProgress> findAllByUserIdAndCourseId(UUID userId, UUID courseId);
}

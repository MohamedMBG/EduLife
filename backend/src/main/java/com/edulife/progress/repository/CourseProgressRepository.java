package com.edulife.progress.repository;

import com.edulife.progress.entity.CourseProgress;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseProgressRepository extends JpaRepository<CourseProgress, UUID> {

    Optional<CourseProgress> findByUserIdAndCourseId(UUID userId, UUID courseId);
}

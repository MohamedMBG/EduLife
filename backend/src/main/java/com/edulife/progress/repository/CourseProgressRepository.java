package com.edulife.progress.repository;

import com.edulife.progress.entity.CourseProgress;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data repository for {@link CourseProgress} aggregate tracking and analytics queries. */
public interface CourseProgressRepository extends JpaRepository<CourseProgress, UUID> {

    Optional<CourseProgress> findByUserIdAndCourseId(UUID userId, UUID courseId);

    // Analytics (read-only): learners who have any progress row for the course.
    long countByCourseId(UUID courseId);

    // Analytics (read-only): learners who completed every lesson. totalLessons > 0 guards
    // against empty courses counting as "completed" when 0 >= 0.
    @Query("select count(cp) from CourseProgress cp "
            + "where cp.courseId = :courseId "
            + "and cp.totalLessons > 0 and cp.completedLessons >= cp.totalLessons")
    long countCompletedByCourseId(@Param("courseId") UUID courseId);
}

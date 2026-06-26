package com.edulife.courses.repository;

import com.edulife.courses.entity.Lesson;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data JPA repository for {@link Lesson} entities with section- and course-scoped queries. */
public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    List<Lesson> findAllByCourseSectionIdOrderByDisplayOrderAsc(UUID courseSectionId);

    /** Finds a lesson by its ID, verifying it belongs to the given course via its section. */
    @Query("SELECT l FROM Lesson l JOIN CourseSection s ON s.id = l.courseSectionId WHERE l.id = :lessonId AND s.courseId = :courseId")
    Optional<Lesson> findByIdAndCourseId(@Param("lessonId") UUID lessonId, @Param("courseId") UUID courseId);

    /** Counts all lessons across all sections of a course. */
    @Query("SELECT COUNT(l) FROM Lesson l JOIN CourseSection s ON s.id = l.courseSectionId WHERE s.courseId = :courseId")
    long countByCourseId(@Param("courseId") UUID courseId);
}

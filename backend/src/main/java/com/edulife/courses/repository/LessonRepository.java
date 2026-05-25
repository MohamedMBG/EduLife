package com.edulife.courses.repository;

import com.edulife.courses.entity.Lesson;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    List<Lesson> findAllByCourseSectionIdOrderByDisplayOrderAsc(UUID courseSectionId);

    @Query("SELECT l FROM Lesson l JOIN CourseSection s ON s.id = l.courseSectionId WHERE l.id = :lessonId AND s.courseId = :courseId")
    Optional<Lesson> findByIdAndCourseId(@Param("lessonId") UUID lessonId, @Param("courseId") UUID courseId);

    @Query("SELECT COUNT(l) FROM Lesson l JOIN CourseSection s ON s.id = l.courseSectionId WHERE s.courseId = :courseId")
    long countByCourseId(@Param("courseId") UUID courseId);
}

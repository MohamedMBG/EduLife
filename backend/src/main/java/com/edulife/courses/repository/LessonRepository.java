package com.edulife.courses.repository;

import com.edulife.courses.entity.Lesson;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    // Lessons must be returned in section order so the learner flow stays predictable
    // without repeating sorting rules in every service method.
    List<Lesson> findAllByCourseSectionIdOrderByDisplayOrderAsc(UUID courseSectionId);
}

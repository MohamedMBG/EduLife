package com.edulife.courses.repository;

import com.edulife.courses.entity.CourseSection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseSectionRepository extends JpaRepository<CourseSection, UUID> {

    // Course detail screens must render sections in learning order, so ordering is enforced
    // directly in the repository method instead of relying on callers to sort manually.
    List<CourseSection> findAllByCourseIdOrderByDisplayOrderAsc(UUID courseId);
}

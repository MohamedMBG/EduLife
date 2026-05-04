package com.edulife.courses.repository;

import com.edulife.courses.entity.Course;
import com.edulife.courses.model.CourseStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    // Discovery always excludes drafts and archived rows at the query level so the service
    // never has to filter unsafe catalog data in memory after fetching it.
    Page<Course> findAllByStatus(CourseStatus status, Pageable pageable);

    // Level filtering stays in the repository query so pagination counts remain correct.
    Page<Course> findAllByStatusAndLevel(CourseStatus status, String level, Pageable pageable);
}

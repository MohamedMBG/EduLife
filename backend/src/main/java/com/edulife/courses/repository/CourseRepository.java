package com.edulife.courses.repository;

import com.edulife.courses.entity.Course;
import com.edulife.courses.model.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    Page<Course> findAllByStatus(CourseStatus status, Pageable pageable);

    Page<Course> findAllByStatusAndLevel(CourseStatus status, String level, Pageable pageable);
}

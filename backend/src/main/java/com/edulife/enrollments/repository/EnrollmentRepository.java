package com.edulife.enrollments.repository;

import com.edulife.enrollments.entity.Enrollment;
import com.edulife.enrollments.model.EnrollmentStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link Enrollment} persistence and query operations. */
public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    boolean existsByUserIdAndCourseId(UUID userId, UUID courseId);

    java.util.Optional<Enrollment> findByUserIdAndCourseId(UUID userId, UUID courseId);

    boolean existsByUserIdAndCourseIdAndStatus(UUID userId, UUID courseId, EnrollmentStatus status);

    List<Enrollment> findAllByUserIdAndStatus(UUID userId, EnrollmentStatus status);

    long countByUserIdAndStatus(UUID userId, EnrollmentStatus status);

    long countByStatus(EnrollmentStatus status);

    // Analytics (read-only): active enrollments for one course, used by teacher course analytics.
    long countByCourseIdAndStatus(UUID courseId, EnrollmentStatus status);
}

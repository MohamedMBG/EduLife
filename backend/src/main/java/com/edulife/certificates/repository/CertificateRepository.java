package com.edulife.certificates.repository;

import com.edulife.certificates.entity.Certificate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for certificates, supporting ownership checks, verification lookups, and analytics counts. */
public interface CertificateRepository extends JpaRepository<Certificate, UUID> {
    List<Certificate> findAllByUserId(UUID userId);
    boolean existsByUserIdAndCourseId(UUID userId, UUID courseId);
    boolean existsByUserIdAndExamAttemptId(UUID userId, UUID examAttemptId);
    long countByUserId(UUID userId);

    // Analytics (read-only): certificates issued for one course, used by teacher course analytics.
    long countByCourseId(UUID courseId);
    Optional<Certificate> findByIdAndUserId(UUID id, UUID userId);
    Optional<Certificate> findByVerificationHash(String verificationHash);
    Optional<Certificate> findByUserIdAndCourseId(UUID userId, UUID courseId);
}

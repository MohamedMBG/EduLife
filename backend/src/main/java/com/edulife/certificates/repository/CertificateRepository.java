package com.edulife.certificates.repository;

import com.edulife.certificates.entity.Certificate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateRepository extends JpaRepository<Certificate, UUID> {
    List<Certificate> findAllByUserId(UUID userId);
    boolean existsByUserIdAndCourseId(UUID userId, UUID courseId);
    long countByUserId(UUID userId);
}

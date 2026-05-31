package com.edulife.teacherrequests.repository;

import com.edulife.teacherrequests.entity.TeacherRequest;
import com.edulife.teacherrequests.model.RequestStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherRequestRepository extends JpaRepository<TeacherRequest, UUID> {

    boolean existsByUserIdAndStatus(UUID userId, RequestStatus status);

    Optional<TeacherRequest> findFirstByUserIdOrderByRequestedAtDesc(UUID userId);

    Page<TeacherRequest> findAllByStatus(RequestStatus status, Pageable pageable);
}

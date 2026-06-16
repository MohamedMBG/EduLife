package com.edulife.advisor.repository;

import com.edulife.advisor.entity.AdvisorLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdvisorLogRepository extends JpaRepository<AdvisorLog, UUID> {}

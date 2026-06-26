package com.edulife.advisor.repository;

import com.edulife.advisor.entity.AdvisorLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for persisting and querying {@link AdvisorLog} audit records. */
public interface AdvisorLogRepository extends JpaRepository<AdvisorLog, UUID> {}

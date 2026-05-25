package com.edulife.exams.repository;

import com.edulife.exams.entity.ExamAttempt;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, UUID> {
    boolean existsByUserIdAndExamIdAndPassedTrue(UUID userId, UUID examId);
}

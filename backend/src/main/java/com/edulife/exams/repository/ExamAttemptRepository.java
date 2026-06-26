package com.edulife.exams.repository;

import com.edulife.exams.entity.ExamAttempt;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data repository for exam attempts, providing pass/fail checks, cooldown queries, and analytics aggregates. */
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, UUID> {
    boolean existsByUserIdAndExamIdAndPassedTrue(UUID userId, UUID examId);

    long countByUserIdAndExamIdAndPassedFalse(UUID userId, UUID examId);

    Optional<ExamAttempt> findTopByUserIdAndExamIdAndPassedFalseOrderByTakenAtDesc(UUID userId, UUID examId);

    // Analytics (read-only) counts. Aggregates over existing attempt rows only — correct
    // answers live in exam_questions/exam_choices and are never touched here.

    // Student summary: this learner's own attempt totals.
    long countByUserId(UUID userId);

    long countByUserIdAndPassedTrue(UUID userId);

    @Query("select coalesce(avg(a.score), 0) from ExamAttempt a where a.userId = :userId")
    Double averageScoreByUserId(@Param("userId") UUID userId);

    @Query("select coalesce(max(a.score), 0) from ExamAttempt a where a.userId = :userId")
    Integer maxScoreByUserId(@Param("userId") UUID userId);

    // Teacher course analytics: attempts for one course's exam (attempt-based pass rate).
    long countByExamId(UUID examId);

    long countByExamIdAndPassedTrue(UUID examId);

    // Platform analytics: global passed-attempt count (total uses JpaRepository.count()).
    long countByPassedTrue();
}

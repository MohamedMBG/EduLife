package com.edulife.exams.repository;

import com.edulife.exams.entity.Exam;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for exams, supporting lookup by course (one exam per course). */
public interface ExamRepository extends JpaRepository<Exam, UUID> {
    Optional<Exam> findByCourseId(UUID courseId);
}

package com.edulife.exams.repository;

import com.edulife.exams.entity.Exam;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamRepository extends JpaRepository<Exam, UUID> {
    Optional<Exam> findByCourseId(UUID courseId);
}

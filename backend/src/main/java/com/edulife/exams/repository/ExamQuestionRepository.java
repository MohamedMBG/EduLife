package com.edulife.exams.repository;

import com.edulife.exams.entity.ExamQuestion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for exam questions, supporting ordered retrieval and bulk deletion by exam. */
public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, UUID> {
    List<ExamQuestion> findAllByExamIdOrderByOrderIndexAsc(UUID examId);
    void deleteAllByExamId(UUID examId);
}

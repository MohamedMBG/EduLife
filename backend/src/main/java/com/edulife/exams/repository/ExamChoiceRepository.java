package com.edulife.exams.repository;

import com.edulife.exams.entity.ExamChoice;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data repository for exam choices, supporting bulk retrieval by question and cascading deletes. */
public interface ExamChoiceRepository extends JpaRepository<ExamChoice, UUID> {

    List<ExamChoice> findAllByQuestionId(UUID questionId);

    @Query("SELECT ec FROM ExamChoice ec WHERE ec.questionId IN :questionIds")
    List<ExamChoice> findAllByQuestionIdIn(@Param("questionIds") List<UUID> questionIds);

    @Modifying
    @Query("DELETE FROM ExamChoice ec WHERE ec.questionId IN :questionIds")
    void deleteAllByQuestionIdIn(@Param("questionIds") List<UUID> questionIds);
}

package com.edulife.exams.dto;

import java.util.List;
import java.util.UUID;

/** Response DTO for an exam, including its questions and choices (correct answers excluded). */
public record ExamDto(
        UUID examId,
        UUID courseId,
        String title,
        int passScore,
        Integer timeLimitMinutes,
        List<QuestionDto> questions
) {
    /** A single exam question with its available choices. */
    public record QuestionDto(
            UUID questionId,
            String questionText,
            int orderIndex,
            List<ChoiceDto> choices
    ) {}

    /** An answer choice; the correctness flag is never exposed to the client. */
    public record ChoiceDto(
            UUID choiceId,
            String choiceText
    ) {}
}

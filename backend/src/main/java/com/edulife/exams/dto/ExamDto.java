package com.edulife.exams.dto;

import java.util.List;
import java.util.UUID;

public record ExamDto(
        UUID examId,
        UUID courseId,
        String title,
        int passScore,
        Integer timeLimitMinutes,
        List<QuestionDto> questions
) {
    public record QuestionDto(
            UUID questionId,
            String questionText,
            int orderIndex,
            List<ChoiceDto> choices
    ) {}

    public record ChoiceDto(
            UUID choiceId,
            String choiceText
    ) {}
}

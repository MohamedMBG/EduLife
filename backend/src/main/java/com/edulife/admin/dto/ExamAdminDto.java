package com.edulife.admin.dto;

import java.util.List;
import java.util.UUID;

/** CMS projection of an exam including all questions and choices with correct-answer flags. */
public record ExamAdminDto(
        UUID id,
        UUID courseId,
        String title,
        int passScore,
        Integer timeLimitMinutes,
        List<QuestionDto> questions
) {

    /** One question with its choices. Unlike the learner-facing ExamDto.QuestionDto,
     *  this includes the isCorrect flag so CMS editors can verify the answer key. */
    public record QuestionDto(
            UUID id,
            String questionText,
            int orderIndex,
            List<ChoiceDto> choices
    ) {}

    public record ChoiceDto(
            UUID id,
            String choiceText,
            boolean correct
    ) {}
}

package com.edulife.exams.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/** Request DTO for submitting exam answers; each answer maps a question to a chosen choice. */
public record SubmitExamRequest(
        @NotEmpty List<@Valid AnswerDto> answers
) {
    /** A single answer pairing a question with the learner's selected choice. */
    public record AnswerDto(
            @NotNull UUID questionId,
            @NotNull UUID choiceId
    ) {}
}

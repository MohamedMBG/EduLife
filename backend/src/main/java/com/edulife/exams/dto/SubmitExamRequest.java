package com.edulife.exams.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record SubmitExamRequest(
        @NotEmpty List<@Valid AnswerDto> answers
) {
    public record AnswerDto(
            @NotNull UUID questionId,
            @NotNull UUID choiceId
    ) {}
}

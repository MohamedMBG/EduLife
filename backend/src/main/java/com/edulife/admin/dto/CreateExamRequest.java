package com.edulife.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Request body for POST /api/v1/cms/courses/{courseId}/exam. Creates exam + all questions atomically. */
public record CreateExamRequest(
        @NotBlank(message = "title is required")
        @Size(max = 200, message = "title must be 200 characters or fewer")
        String title,

        // passScore is a percentage threshold (0–100). Project rule: 80 is the default.
        @NotNull(message = "passScore is required")
        @Min(value = 1, message = "passScore must be at least 1")
        @Max(value = 100, message = "passScore must be at most 100")
        Integer passScore,

        @Min(value = 1, message = "timeLimitMinutes must be at least 1")
        Integer timeLimitMinutes,

        @NotEmpty(message = "at least one question is required")
        @Valid
        List<QuestionRequest> questions
) {

    /** One MCQ question with its answer choices. */
    public record QuestionRequest(
            @NotBlank(message = "questionText is required")
            String questionText,

            @NotNull(message = "orderIndex is required")
            @Min(value = 1, message = "orderIndex must be at least 1")
            Integer orderIndex,

            // At least 2 choices are needed for a meaningful MCQ. Exactly one must be correct.
            @NotEmpty(message = "at least one choice is required")
            @Valid
            List<ChoiceRequest> choices
    ) {}

    /** One answer choice for a question. */
    public record ChoiceRequest(
            @NotBlank(message = "choiceText is required")
            String choiceText,

            boolean correct
    ) {}
}

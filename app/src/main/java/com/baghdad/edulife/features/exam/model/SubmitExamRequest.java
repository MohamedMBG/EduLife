package com.baghdad.edulife.features.exam.model;

import java.util.List;

public class SubmitExamRequest {
    public final List<AnswerDto> answers;

    public SubmitExamRequest(List<AnswerDto> answers) {
        this.answers = answers;
    }

    public static class AnswerDto {
        public final String questionId;
        public final String choiceId;

        public AnswerDto(String questionId, String choiceId) {
            this.questionId = questionId;
            this.choiceId = choiceId;
        }
    }
}

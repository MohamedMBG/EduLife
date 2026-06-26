package com.baghdad.edulife.features.courses.model;

import java.util.List;

/** Request body sent to the backend when a learner submits their exam answers for server-side scoring. */
public class SubmitExamRequest {
    public List<AnswerItem> answers;

    public SubmitExamRequest(List<AnswerItem> answers) {
        this.answers = answers;
    }

    public static class AnswerItem {
        public String questionId;
        public String choiceId;

        public AnswerItem(String questionId, String choiceId) {
            this.questionId = questionId;
            this.choiceId = choiceId;
        }
    }
}

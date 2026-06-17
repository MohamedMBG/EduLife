package com.baghdad.edulife.features.exams.model;

import java.util.List;

public class CmsExamQuestionRequest {
    public String questionText;
    public int orderIndex;
    public List<CmsExamChoiceRequest> choices;

    public CmsExamQuestionRequest(String questionText, int orderIndex,
                                  List<CmsExamChoiceRequest> choices) {
        this.questionText = questionText;
        this.orderIndex = orderIndex;
        this.choices = choices;
    }
}

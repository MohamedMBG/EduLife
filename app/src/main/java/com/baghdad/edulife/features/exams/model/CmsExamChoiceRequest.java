package com.baghdad.edulife.features.exams.model;

public class CmsExamChoiceRequest {
    public String choiceText;
    public boolean correct;

    public CmsExamChoiceRequest(String choiceText, boolean correct) {
        this.choiceText = choiceText;
        this.correct = correct;
    }
}

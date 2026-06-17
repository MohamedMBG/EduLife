package com.baghdad.edulife.features.exams.model;

import java.util.List;

public class CmsExamRequest {
    public String title;
    public int passScore;
    public Integer timeLimitMinutes;
    public List<CmsExamQuestionRequest> questions;

    public CmsExamRequest(String title, int passScore, Integer timeLimitMinutes,
                          List<CmsExamQuestionRequest> questions) {
        this.title = title;
        this.passScore = passScore;
        this.timeLimitMinutes = timeLimitMinutes;
        this.questions = questions;
    }
}

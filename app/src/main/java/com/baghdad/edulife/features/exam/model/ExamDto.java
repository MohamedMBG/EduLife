package com.baghdad.edulife.features.exam.model;

import java.util.List;

public class ExamDto {
    public String examId;
    public String courseId;
    public String title;
    public int passScore;
    public Integer timeLimitMinutes;
    public List<QuestionDto> questions;

    public static class QuestionDto {
        public String questionId;
        public String questionText;
        public int orderIndex;
        public List<ChoiceDto> choices;
    }

    public static class ChoiceDto {
        public String choiceId;
        public String choiceText;
    }
}

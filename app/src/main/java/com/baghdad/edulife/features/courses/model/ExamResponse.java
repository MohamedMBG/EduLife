package com.baghdad.edulife.features.courses.model;

import java.util.List;

public class ExamResponse {
    public String examId;
    public String courseId;
    public String title;
    public int passScore;
    public Integer timeLimitMinutes;
    public List<ExamQuestion> questions;
}

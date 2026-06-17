package com.baghdad.edulife.features.exams.model;

import java.util.List;

public class CmsExamResponse {
    public String id;
    public String courseId;
    public String title;
    public int passScore;
    public Integer timeLimitMinutes;
    public List<CmsExamQuestion> questions;
}

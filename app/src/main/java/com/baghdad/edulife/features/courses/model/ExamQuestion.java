package com.baghdad.edulife.features.courses.model;

import java.util.List;

/** A single MCQ question within an exam, containing its text, display order, and available choices. */
public class ExamQuestion {
    public String questionId;
    public String questionText;
    public int orderIndex;
    public List<ExamChoice> choices;
}

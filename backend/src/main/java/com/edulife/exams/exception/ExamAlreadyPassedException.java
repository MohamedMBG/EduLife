package com.edulife.exams.exception;

public class ExamAlreadyPassedException extends RuntimeException {
    public ExamAlreadyPassedException() {
        super("You have already passed this exam and cannot retake it");
    }
}

package com.edulife.exams.exception;

/** Thrown when a learner attempts to retake an exam they have already passed. */
public class ExamAlreadyPassedException extends RuntimeException {
    public ExamAlreadyPassedException() {
        super("You have already passed this exam and cannot retake it");
    }
}

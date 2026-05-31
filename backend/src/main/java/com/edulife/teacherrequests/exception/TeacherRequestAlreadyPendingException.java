package com.edulife.teacherrequests.exception;

public class TeacherRequestAlreadyPendingException extends RuntimeException {
    public TeacherRequestAlreadyPendingException() {
        super("A pending teacher request already exists for this account");
    }
}

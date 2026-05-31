package com.edulife.teacherrequests.exception;

public class TeacherRequestNotPendingException extends RuntimeException {
    public TeacherRequestNotPendingException() {
        super("This teacher request is no longer pending");
    }
}

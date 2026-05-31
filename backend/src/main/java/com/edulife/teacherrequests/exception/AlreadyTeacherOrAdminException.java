package com.edulife.teacherrequests.exception;

public class AlreadyTeacherOrAdminException extends RuntimeException {
    public AlreadyTeacherOrAdminException() {
        super("Account already has teacher or admin privileges");
    }
}

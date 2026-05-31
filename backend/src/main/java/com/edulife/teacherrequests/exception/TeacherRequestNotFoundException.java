package com.edulife.teacherrequests.exception;

public class TeacherRequestNotFoundException extends RuntimeException {
    public TeacherRequestNotFoundException() {
        super("Teacher request not found");
    }
}

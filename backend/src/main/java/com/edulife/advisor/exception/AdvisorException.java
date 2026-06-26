package com.edulife.advisor.exception;

/** Runtime exception for advisor-specific errors such as LLM failures or invalid recommendations. */
public class AdvisorException extends RuntimeException {

    public AdvisorException(String message) {
        super(message);
    }

    public AdvisorException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.edulife.certificates.exception;

public class CertificateAccessDeniedException extends RuntimeException {
    public CertificateAccessDeniedException(String message) {
        super(message);
    }
}

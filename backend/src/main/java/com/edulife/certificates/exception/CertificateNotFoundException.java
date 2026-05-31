package com.edulife.certificates.exception;

public class CertificateNotFoundException extends RuntimeException {
    public CertificateNotFoundException(String message) {
        super(message);
    }
}

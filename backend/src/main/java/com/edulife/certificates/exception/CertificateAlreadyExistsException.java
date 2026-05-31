package com.edulife.certificates.exception;

public class CertificateAlreadyExistsException extends RuntimeException {
    public CertificateAlreadyExistsException(String message) {
        super(message);
    }
}

package com.edulife.certificates.exception;

/** Thrown when a requested certificate does not exist in the database. */
public class CertificateNotFoundException extends RuntimeException {
    public CertificateNotFoundException(String message) {
        super(message);
    }
}

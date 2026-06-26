package com.edulife.certificates.exception;

/** Thrown when certificate generation is requested but one already exists for the user-course pair. */
public class CertificateAlreadyExistsException extends RuntimeException {
    public CertificateAlreadyExistsException(String message) {
        super(message);
    }
}

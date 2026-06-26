package com.edulife.certificates.exception;

/** Thrown when a learner attempts to access a certificate that belongs to another user. */
public class CertificateAccessDeniedException extends RuntimeException {
    public CertificateAccessDeniedException(String message) {
        super(message);
    }
}

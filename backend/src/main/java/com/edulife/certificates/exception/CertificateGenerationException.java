package com.edulife.certificates.exception;

/** Thrown when certificate PDF generation or verification hash computation fails. */
public class CertificateGenerationException extends RuntimeException {
    public CertificateGenerationException(String message) {
        super(message);
    }

    public CertificateGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}

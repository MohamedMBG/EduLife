package com.edulife.certificates.exception;

/**
 * Raised when a certificate row exists and is owned by the caller, but is not in a downloadable
 * state (for example a corrupted row missing its verification hash). Maps to HTTP 409.
 */
public class CertificateNotDownloadableException extends RuntimeException {
    public CertificateNotDownloadableException(String message) {
        super(message);
    }
}

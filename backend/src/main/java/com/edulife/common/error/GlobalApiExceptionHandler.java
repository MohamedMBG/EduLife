package com.edulife.common.error;

import com.edulife.certificates.exception.CertificateAccessDeniedException;
import com.edulife.certificates.exception.CertificateAlreadyExistsException;
import com.edulife.certificates.exception.CertificateGenerationException;
import com.edulife.certificates.exception.CertificateNotFoundException;
import com.edulife.exams.exception.ExamAlreadyPassedException;
import com.edulife.exams.exception.ExamCooldownException;
import com.edulife.teacherrequests.exception.AlreadyTeacherOrAdminException;
import com.edulife.teacherrequests.exception.TeacherRequestAlreadyPendingException;
import com.edulife.teacherrequests.exception.TeacherRequestNotFoundException;
import com.edulife.teacherrequests.exception.TeacherRequestNotPendingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalApiExceptionHandler.class);

    @ExceptionHandler(CertificateNotFoundException.class)
    public ResponseEntity<ApiError> handleCertificateNotFound(CertificateNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(CertificateAccessDeniedException.class)
    public ResponseEntity<ApiError> handleCertificateAccessDenied(CertificateAccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(CertificateAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleCertificateAlreadyExists(CertificateAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(CertificateGenerationException.class)
    public ResponseEntity<ApiError> handleCertificateGeneration(CertificateGenerationException ex) {
        log.error("Certificate generation failed.", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Certificate generation failed");
    }

    @ExceptionHandler(ExamAlreadyPassedException.class)
    public ResponseEntity<ApiError> handleExamAlreadyPassed(ExamAlreadyPassedException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ExamCooldownException.class)
    public ResponseEntity<ExamCooldownError> handleExamCooldown(ExamCooldownException ex) {
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ExamCooldownError.of(ex.getMessage(), ex.getCooldownEndsAt()));
    }

    @ExceptionHandler(TeacherRequestNotFoundException.class)
    public ResponseEntity<ApiError> handleTeacherRequestNotFound(TeacherRequestNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(TeacherRequestAlreadyPendingException.class)
    public ResponseEntity<ApiError> handleTeacherRequestAlreadyPending(TeacherRequestAlreadyPendingException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(AlreadyTeacherOrAdminException.class)
    public ResponseEntity<ApiError> handleAlreadyTeacherOrAdmin(AlreadyTeacherOrAdminException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(TeacherRequestNotPendingException.class)
    public ResponseEntity<ApiError> handleTeacherRequestNotPending(TeacherRequestNotPendingException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException exception) {
        // Invalid client input should return the public API contract instead of a framework stack trace.
        return build(HttpStatus.BAD_REQUEST, safeMessage(exception, "Invalid request"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataConflict(DataIntegrityViolationException exception) {
        // Database uniqueness/constraint failures are controlled conflicts, not unhandled crashes.
        log.warn("Data integrity violation handled as API conflict.", exception);
        return build(HttpStatus.CONFLICT, "Request conflicts with existing data");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        // ResponseStatusException already carries a safe public-facing reason, so prefer it
        // over the framework-generated message that includes the HTTP enum name.
        String reason = exception.getReason();
        String message = (reason == null || reason.isBlank())
                ? safeMessage(exception, status.getReasonPhrase())
                : reason;

        return build(status, message);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException exception) {
        // @PreAuthorize denials would otherwise bubble to the generic 500 handler; instead they
        // share the same 403 contract as the SecurityFilterChain's accessDeniedHandler.
        return build(HttpStatus.FORBIDDEN, "Access denied");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        // Bean Validation failures should surface as 400 with the first field error so clients
        // get a stable, actionable message instead of a 500 stack trace.
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("Invalid request");
        return build(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUpload(MaxUploadSizeExceededException exception) {
        // Map Spring's multipart size violation onto the public 413 contract so clients can
        // surface a stable error code for the avatar upload limit.
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded file exceeds the maximum allowed size");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        // Unexpected errors are logged internally while clients receive a stable, non-sensitive response.
        log.error("Unhandled backend exception.", exception);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiError.of(status, message));
    }

    private String safeMessage(Exception exception, String fallback) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? fallback : message;
    }
}

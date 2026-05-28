package com.edulife.common.error;

import org.springframework.http.HttpStatus;

/**
 * Stable string codes returned alongside every API error so clients can branch on machine-readable
 * values instead of parsing human-facing messages. The HTTP status remains for transport-layer use.
 */
public enum ApiErrorCode {
    VALIDATION_ERROR,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT,
    UNSUPPORTED_MEDIA_TYPE,
    PAYLOAD_TOO_LARGE,
    UNPROCESSABLE,
    RATE_LIMITED,
    INTERNAL_ERROR;

    public static ApiErrorCode forStatus(HttpStatus status) {
        // Default mapping keeps every documented status pinned to a single code so the contract
        // table in #282 stays the single source of truth.
        return switch (status) {
            case BAD_REQUEST -> VALIDATION_ERROR;
            case UNAUTHORIZED -> UNAUTHORIZED;
            case FORBIDDEN -> FORBIDDEN;
            case NOT_FOUND -> NOT_FOUND;
            case CONFLICT -> CONFLICT;
            case UNSUPPORTED_MEDIA_TYPE -> UNSUPPORTED_MEDIA_TYPE;
            case PAYLOAD_TOO_LARGE -> PAYLOAD_TOO_LARGE;
            case UNPROCESSABLE_ENTITY -> UNPROCESSABLE;
            case TOO_MANY_REQUESTS -> RATE_LIMITED;
            default -> INTERNAL_ERROR;
        };
    }
}

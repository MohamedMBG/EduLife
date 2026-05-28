package com.edulife.common.error;

import java.time.Instant;
import org.springframework.http.HttpStatus;

public record ApiError(
        int status,
        String code,
        String message,
        Instant timestamp
) {

    public static ApiError of(HttpStatus status, String message) {
        return of(status, ApiErrorCode.forStatus(status), message);
    }

    public static ApiError of(HttpStatus status, ApiErrorCode code, String message) {
        return new ApiError(status.value(), code.name(), message, Instant.now());
    }
}

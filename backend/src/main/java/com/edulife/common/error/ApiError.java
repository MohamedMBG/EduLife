package com.edulife.common.error;

import java.time.Instant;
import org.springframework.http.HttpStatus;

public record ApiError(
        int status,
        String message,
        Instant timestamp
) {

    public static ApiError of(HttpStatus status, String message) {
        return new ApiError(status.value(), message, Instant.now());
    }
}

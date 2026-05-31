package com.edulife.common.error;

import java.time.Instant;
import org.springframework.http.HttpStatus;

public record ExamCooldownError(
        int status,
        String code,
        String message,
        Instant timestamp,
        Instant cooldownEndsAt
) {
    public static ExamCooldownError of(String message, Instant cooldownEndsAt) {
        return new ExamCooldownError(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                ApiErrorCode.forStatus(HttpStatus.TOO_MANY_REQUESTS).name(),
                message,
                Instant.now(),
                cooldownEndsAt
        );
    }
}

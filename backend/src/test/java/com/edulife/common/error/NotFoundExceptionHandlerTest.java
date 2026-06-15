package com.edulife.common.error;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression: a missing controller mapping was surfacing as 500 "Internal server error" through
 * the generic {@code @ExceptionHandler(Exception.class)}, which masked stale-deploy bugs (e.g. a
 * frontend calling /api/v1/analytics before the backend had the endpoint). The dedicated 404
 * handler ensures unknown API paths return the documented not-found contract instead.
 */
class NotFoundExceptionHandlerTest {

    private final GlobalApiExceptionHandler handler = new GlobalApiExceptionHandler();

    @Test
    void noHandlerFoundReturnsApi404() {
        ResponseEntity<ApiError> response = handler.handleNotFound(
                new NoHandlerFoundException("GET", "/api/v1/missing", null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("Resource not found");
    }

    @Test
    void noResourceFoundReturnsApi404() {
        ResponseEntity<ApiError> response = handler.handleNotFound(
                new NoResourceFoundException(org.springframework.http.HttpMethod.GET, "/missing.txt"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("Resource not found");
    }
}

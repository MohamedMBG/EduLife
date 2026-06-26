package com.edulife.common.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/** Writes the standardized {@link ApiError} JSON response from security filters outside MVC. */
@Component
public class ApiErrorWriter {

    private final ObjectMapper objectMapper;

    public ApiErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Serializes an {@link ApiError} directly to the servlet response with the given status and message. */
    public void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        // Security filters run before MVC exception handling, so they need the same JSON contract written explicitly.
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiError.of(status, message));
    }
}

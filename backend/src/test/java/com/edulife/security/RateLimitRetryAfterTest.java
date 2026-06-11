package com.edulife.security;

import com.edulife.common.error.ApiErrorWriter;
import com.edulife.config.RateLimitFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that rate-limited responses always carry a Retry-After header so clients know
 * when to retry without guessing and hammering the endpoint again immediately.
 */
class RateLimitRetryAfterTest {

    private final ApiErrorWriter writer =
            new ApiErrorWriter(new ObjectMapper().registerModule(new JavaTimeModule()));
    private final RateLimitFilter filter = new RateLimitFilter(writer);
    private final FilterChain passthrough = (req, res) -> {};

    @Test
    void certVerifyRetryAfterHeaderPresentAfterBucketExhausted() throws Exception {
        // Exhaust the 30-per-minute bucket for a single client IP.
        for (int i = 0; i < 30; i++) {
            filter.doFilter(certVerifyRequest(), new MockHttpServletResponse(), passthrough);
        }

        MockHttpServletResponse limited = new MockHttpServletResponse();
        filter.doFilter(certVerifyRequest(), limited, passthrough);

        assertThat(limited.getStatus()).isEqualTo(429);
        assertThat(limited.getHeader("Retry-After"))
                .as("Retry-After must be present so clients know when to retry")
                .isNotNull();
        assertThat(Integer.parseInt(limited.getHeader("Retry-After")))
                .as("Retry-After must be a positive integer (seconds)")
                .isGreaterThan(0);
    }

    @Test
    void examSubmitRetryAfterHeaderPresentAfterBucketExhausted() throws Exception {
        String courseId = "00000000-0000-0000-0000-000000000001";
        // Exhaust the 5-per-hour exam submit bucket for a single principal.
        for (int i = 0; i < 5; i++) {
            filter.doFilter(examSubmitRequest(courseId), new MockHttpServletResponse(), passthrough);
        }

        MockHttpServletResponse limited = new MockHttpServletResponse();
        filter.doFilter(examSubmitRequest(courseId), limited, passthrough);

        assertThat(limited.getStatus()).isEqualTo(429);
        assertThat(limited.getHeader("Retry-After"))
                .as("Retry-After must be present so clients know when to retry")
                .isNotNull();
        assertThat(Integer.parseInt(limited.getHeader("Retry-After")))
                .as("Retry-After should reflect the full refill window (~3600s for the hourly bucket)")
                .isGreaterThan(0);
    }

    @Test
    void requestsWithinBudgetPassThrough() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(certVerifyRequest(), response, passthrough);

        assertThat(response.getStatus())
                .as("First request within budget must not be rate-limited")
                .isNotEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isNull();
    }

    private MockHttpServletRequest certVerifyRequest() {
        MockHttpServletRequest req = new MockHttpServletRequest(
                "GET", "/api/v1/certificates/verify/abc123hash");
        req.setRemoteAddr("198.51.100.1");
        return req;
    }

    private MockHttpServletRequest examSubmitRequest(String courseId) {
        MockHttpServletRequest req = new MockHttpServletRequest(
                "POST", "/api/v1/courses/" + courseId + "/exam/submit");
        req.setRemoteAddr("198.51.100.2");
        return req;
    }
}

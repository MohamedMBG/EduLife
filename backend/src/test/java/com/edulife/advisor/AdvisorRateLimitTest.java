package com.edulife.advisor;

import com.edulife.common.error.ApiErrorWriter;
import com.edulife.config.RateLimitFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class AdvisorRateLimitTest {

    private final ApiErrorWriter writer =
            new ApiErrorWriter(new ObjectMapper().registerModule(new JavaTimeModule()));
    // Each test gets a fresh filter instance so bucket state does not bleed between tests.
    private RateLimitFilter filter;
    private final FilterChain passthrough = (req, res) -> {};

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter(writer);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void tenRequestsAreAllowed() throws Exception {
        for (int i = 0; i < 10; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(advisorRequest("user-A"), response, passthrough);
            assertThat(response.getStatus())
                    .as("Request %d should not be rate-limited", i + 1)
                    .isNotEqualTo(429);
        }
    }

    @Test
    void eleventhRequestReturns429() throws Exception {
        for (int i = 0; i < 10; i++) {
            filter.doFilter(advisorRequest("user-B"), new MockHttpServletResponse(), passthrough);
        }

        MockHttpServletResponse limited = new MockHttpServletResponse();
        filter.doFilter(advisorRequest("user-B"), limited, passthrough);

        assertThat(limited.getStatus()).isEqualTo(429);
    }

    @Test
    void retryAfterHeaderPresentOn429() throws Exception {
        for (int i = 0; i < 10; i++) {
            filter.doFilter(advisorRequest("user-C"), new MockHttpServletResponse(), passthrough);
        }

        MockHttpServletResponse limited = new MockHttpServletResponse();
        filter.doFilter(advisorRequest("user-C"), limited, passthrough);

        assertThat(limited.getHeader("Retry-After"))
                .as("Retry-After must be present so client knows when to retry")
                .isNotNull();
        assertThat(Integer.parseInt(limited.getHeader("Retry-After")))
                .isGreaterThan(0);
    }

    @Test
    void rateLimitIsPerUser() throws Exception {
        // Exhaust user-D's bucket
        for (int i = 0; i < 10; i++) {
            filter.doFilter(advisorRequest("user-D"), new MockHttpServletResponse(), passthrough);
        }

        // user-E still has a fresh bucket — should not be affected
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(advisorRequest("user-E"), response, passthrough);

        assertThat(response.getStatus())
                .as("Different user must have an independent bucket")
                .isNotEqualTo(429);
    }

    @Test
    void authenticatedPrincipalUsedAsBucketKey() throws Exception {
        // Set an authenticated principal in the SecurityContext
        setAuthenticatedPrincipal("firebase-uid-X");

        // Exhaust bucket for firebase-uid-X
        for (int i = 0; i < 10; i++) {
            filter.doFilter(advisorRequestNoIp(), new MockHttpServletResponse(), passthrough);
        }
        MockHttpServletResponse limited = new MockHttpServletResponse();
        filter.doFilter(advisorRequestNoIp(), limited, passthrough);

        assertThat(limited.getStatus()).isEqualTo(429);

        // Switch to a different authenticated user — independent bucket
        setAuthenticatedPrincipal("firebase-uid-Y");
        MockHttpServletResponse fresh = new MockHttpServletResponse();
        filter.doFilter(advisorRequestNoIp(), fresh, passthrough);

        assertThat(fresh.getStatus()).isNotEqualTo(429);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Simulates a request from a distinct user via remote address (principal fallback). */
    private MockHttpServletRequest advisorRequest(String userKey) {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/advisor/recommend");
        req.setRemoteAddr(userKey);
        return req;
    }

    /** Request with a fixed remote address; principal comes from SecurityContextHolder. */
    private MockHttpServletRequest advisorRequestNoIp() {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/advisor/recommend");
        req.setRemoteAddr("10.0.0.1");
        return req;
    }

    private void setAuthenticatedPrincipal(String uid) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(uid, null, java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}

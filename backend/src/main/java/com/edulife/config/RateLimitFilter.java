package com.edulife.config;

import com.edulife.common.error.ApiErrorWriter;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Token-bucket rate limiting for the three endpoints most vulnerable to abuse:
 *  - POST /api/v1/auth/sync        30 calls / minute  per principal
 *  - POST /api/v1/enrollments      20 calls / hour    per principal
 *  - POST /api/v1/courses/{courseId}/exam/submit  5 calls / hour per principal
 *
 * Buckets are held in a ConcurrentHashMap (in-memory, single-instance only).
 * If horizontal scaling is added, swap the backing store to bucket4j-redis.
 *
 * This filter is NOT annotated @Component to avoid Spring Boot auto-registering it as
 * a plain servlet filter in addition to the Spring Security chain. It is registered
 * explicitly in SecurityConfig via addFilterAfter(rateLimitFilter, FirebaseTokenFilter.class).
 */
public class RateLimitFilter extends OncePerRequestFilter {

    // Patterns are compiled once at startup rather than per-request to avoid regex overhead.
    private static final Pattern EXAM_SUBMIT_PATTERN =
            Pattern.compile("^/api/v1/courses/[^/]+/exam/submit$");

    // Bandwidth limits define the token-bucket capacity and refill rate.
    // Using Bandwidth.classic with Refill.intervally: the full capacity is restored as a
    // single burst at the end of each period (not gradually). This is intentional — auth/sync
    // and enrollment are periodic actions, not streaming calls.
    private static final Bandwidth SYNC_LIMIT =
            Bandwidth.classic(30, Refill.intervally(30, Duration.ofMinutes(1)));
    private static final Bandwidth ENROLL_LIMIT =
            Bandwidth.classic(20, Refill.intervally(20, Duration.ofHours(1)));
    private static final Bandwidth EXAM_LIMIT =
            Bandwidth.classic(5, Refill.intervally(5, Duration.ofHours(1)));

    // Buckets are keyed by "prefix:principal" so each user has independent limits per endpoint.
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    private final ApiErrorWriter apiErrorWriter;

    public RateLimitFilter(ApiErrorWriter apiErrorWriter) {
        this.apiErrorWriter = apiErrorWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        Bucket bucket = resolveBucket(request);

        if (bucket != null && !bucket.tryConsume(1)) {
            // Return the shared ApiError contract so clients can branch on RATE_LIMITED code.
            apiErrorWriter.write(response, HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
            return;
        }

        chain.doFilter(request, response);
    }

    private Bucket resolveBucket(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) return null;

        String path = request.getRequestURI();
        String principal = resolvePrincipal(request);

        if ("/api/v1/auth/sync".equals(path)) {
            return buckets.computeIfAbsent("sync:" + principal,
                    k -> Bucket.builder().addLimit(SYNC_LIMIT).build());
        }

        if ("/api/v1/enrollments".equals(path)) {
            return buckets.computeIfAbsent("enroll:" + principal,
                    k -> Bucket.builder().addLimit(ENROLL_LIMIT).build());
        }

        if (EXAM_SUBMIT_PATTERN.matcher(path).matches()) {
            return buckets.computeIfAbsent("exam:" + principal,
                    k -> Bucket.builder().addLimit(EXAM_LIMIT).build());
        }

        return null;
    }

    /**
     * Returns the authenticated principal identifier or the remote IP as a fallback.
     * FirebaseTokenFilter runs before this filter and populates the SecurityContext,
     * so the principal is available for all three rate-limited endpoints.
     */
    private static String resolvePrincipal(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
            return auth.getPrincipal().toString();
        }
        // IP fallback for unauthenticated requests that somehow reach a rate-limited path.
        return request.getRemoteAddr();
    }
}

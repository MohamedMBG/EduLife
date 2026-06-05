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
 * Token-bucket rate limiting for the endpoints most vulnerable to abuse:
 *  - POST /api/v1/auth/sync                              30 calls / minute  per principal
 *  - POST /api/v1/enrollments                            20 calls / hour    per principal
 *  - POST /api/v1/courses/{courseId}/exam/submit          5 calls / hour    per principal
 *  - GET  /api/v1/certificates/verify/{hash}             30 calls / minute  per client IP
 *
 * The certificate verify endpoint is public (no Firebase auth), so a per-IP bucket prevents
 * brute-force enumeration of verification hashes by unauthenticated clients.
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
    private static final Pattern CERT_VERIFY_PATTERN =
            Pattern.compile("^/api/v1/certificates/verify/[^/]+$");

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
    // Cert verify is the only public endpoint behind this filter; per-IP cap defends against
    // hash enumeration without blocking legitimate employer/institution checks.
    private static final Bandwidth CERT_VERIFY_LIMIT =
            Bandwidth.classic(30, Refill.intervally(30, Duration.ofMinutes(1)));

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
        String method = request.getMethod();
        String path = request.getRequestURI();

        // GET /certificates/verify is public, so the bucket key must be the client IP
        // rather than an authenticated principal that will never be populated here.
        if ("GET".equalsIgnoreCase(method) && CERT_VERIFY_PATTERN.matcher(path).matches()) {
            String ip = resolveClientIp(request);
            return buckets.computeIfAbsent("certverify:" + ip,
                    k -> Bucket.builder().addLimit(CERT_VERIFY_LIMIT).build());
        }

        if (!"POST".equalsIgnoreCase(method)) return null;

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
     * Resolves the client IP from X-Forwarded-For (set by the platform proxy) and falls back
     * to the raw remote address for direct connections. Only the first forwarded hop is used
     * since intermediate proxies can append untrusted values to the chain.
     */
    private static String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
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

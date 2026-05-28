package com.edulife.security;

import com.edulife.common.error.ApiErrorWriter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class FirebaseTokenFilter extends OncePerRequestFilter {

    private static final RequestMatcher PUBLIC_ENDPOINTS = new OrRequestMatcher(
            new AntPathRequestMatcher("/actuator/health"),
            // Avatar files are public assets served by URL to img tags and the web cache, so
            // the Firebase filter must not block them with a malformed-header response.
            new AntPathRequestMatcher("/uploads/avatars/**")
    );

    private final FirebaseAuth firebaseAuth;
    private final ApiErrorWriter apiErrorWriter;

    public FirebaseTokenFilter(FirebaseAuth firebaseAuth, ApiErrorWriter apiErrorWriter) {
        this.firebaseAuth = firebaseAuth;
        this.apiErrorWriter = apiErrorWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Public routes must stay reachable without Firebase auth so health checks and explicit public APIs do not fail on missing headers.
        return PUBLIC_ENDPOINTS.matches(request);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            // Spring Security will convert missing credentials into the shared 401 contract via
            // the configured authentication entry point, so the filter should not write here.
            filterChain.doFilter(request, response);
            return;
        }

        if (!authorizationHeader.startsWith("Bearer ")) {
            apiErrorWriter.write(response, HttpStatus.UNAUTHORIZED, "Malformed Authorization header");
            return;
        }

        String idToken = authorizationHeader.substring(7);

        try {
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);

            Boolean emailVerified = decodedToken.isEmailVerified();

            if (!Boolean.TRUE.equals(emailVerified)) {
                // Verified email is a locked learner-flow rule from Sprint 1, so discovery
                // cannot proceed even if the token itself is otherwise valid.
                apiErrorWriter.write(response, HttpStatus.FORBIDDEN, "Email is not verified");
                return;
            }

            String firebaseUid = decodedToken.getUid();
            String email = decodedToken.getEmail();

            FirebaseAuthentication authentication =
                    new FirebaseAuthentication(firebaseUid, email);

            // Only trusted Firebase claims are copied into the security context. Client-supplied
            // user IDs or roles are never accepted here.
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (FirebaseAuthException e) {
            SecurityContextHolder.clearContext();
            apiErrorWriter.write(response, HttpStatus.UNAUTHORIZED, "Invalid or expired Firebase token");
        }
    }
}

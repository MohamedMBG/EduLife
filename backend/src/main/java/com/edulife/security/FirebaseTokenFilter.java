package com.edulife.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class FirebaseTokenFilter extends OncePerRequestFilter {

    private static final RequestMatcher PUBLIC_ENDPOINTS = new AntPathRequestMatcher("/actuator/health");

    private final FirebaseAuth firebaseAuth;

    public FirebaseTokenFilter(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
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
            filterChain.doFilter(request, response);
            return;
        }

        if (!authorizationHeader.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Malformed Authorization header");
            return;
        }

        String idToken = authorizationHeader.substring(7);

        try {
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);

            Boolean emailVerified = decodedToken.isEmailVerified();

            if (!Boolean.TRUE.equals(emailVerified)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Email is not verified");
                return;
            }

            String firebaseUid = decodedToken.getUid();
            String email = decodedToken.getEmail();

            FirebaseAuthentication authentication =
                    new FirebaseAuthentication(firebaseUid, email);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (FirebaseAuthException e) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired Firebase token");
        }
    }
}

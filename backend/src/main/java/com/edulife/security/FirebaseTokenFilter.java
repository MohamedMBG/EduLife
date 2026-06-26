package com.edulife.security;

import com.edulife.common.error.ApiErrorWriter;
import com.edulife.users.entity.User;
import com.edulife.users.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that validates Firebase ID tokens from the Authorization header
 * and populates the Spring SecurityContext with a {@link FirebaseAuthentication}.
 *
 * <p>Rejects requests with unverified emails and resolves user roles from the internal
 * user table rather than trusting client-supplied claims.</p>
 */
public class FirebaseTokenFilter extends OncePerRequestFilter {

    private static final RequestMatcher PUBLIC_ENDPOINTS = new OrRequestMatcher(
            new AntPathRequestMatcher("/actuator/health"),
            new AntPathRequestMatcher("/uploads/avatars/**"),
            new AntPathRequestMatcher("/uploads/course-covers/**")
    );

    private final FirebaseAuth firebaseAuth;
    private final ApiErrorWriter apiErrorWriter;
    private final UserRepository userRepository;

    public FirebaseTokenFilter(
            FirebaseAuth firebaseAuth,
            ApiErrorWriter apiErrorWriter,
            UserRepository userRepository
    ) {
        this.firebaseAuth = firebaseAuth;
        this.apiErrorWriter = apiErrorWriter;
        this.userRepository = userRepository;
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
            String displayName = decodedToken.getName();

            // Role resolution must come from the trusted users table, not from the Firebase token.
            // Unsynced users get no role authorities so role-gated endpoints return 403 until /auth/sync runs.
            FirebaseAuthentication authentication = userRepository.findByFirebaseUid(firebaseUid)
                    .map(user -> new FirebaseAuthentication(firebaseUid, email, displayName, authoritiesFor(user)))
                    .orElseGet(() -> new FirebaseAuthentication(firebaseUid, email, displayName));

            // Only trusted Firebase claims are copied into the security context. Client-supplied
            // user IDs or roles are never accepted here.
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (FirebaseAuthException e) {
            SecurityContextHolder.clearContext();
            apiErrorWriter.write(response, HttpStatus.UNAUTHORIZED, "Invalid or expired Firebase token");
        }
    }

    private static List<SimpleGrantedAuthority> authoritiesFor(User user) {
        // Spring Security's hasRole() expects the ROLE_ prefix, so the authority must include it
        // explicitly to match @PreAuthorize("hasAnyRole('TEACHER','ADMIN')") expressions.
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }
}

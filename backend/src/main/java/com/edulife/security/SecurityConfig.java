package com.edulife.security;

import com.edulife.common.error.ApiErrorWriter;
import com.edulife.users.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    // The filter is exposed as a bean so the same Firebase validation rule is reused across
    // all protected API modules instead of being duplicated per controller or test slice.
    public FirebaseTokenFilter firebaseTokenFilter(
            FirebaseAuth firebaseAuth,
            ApiErrorWriter apiErrorWriter,
            UserRepository userRepository
    ) {
        return new FirebaseTokenFilter(firebaseAuth, apiErrorWriter, userRepository);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            FirebaseTokenFilter firebaseTokenFilter,
            ApiErrorWriter apiErrorWriter
    ) throws Exception {

        return http
                // EduLife mobile clients use Bearer tokens only, so CSRF protection for cookie
                // sessions would add noise without improving the current threat model.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        // Avatar files are served as plain URLs to clients that may not carry the
                        // Firebase token (img tags, web cache). Filenames are UUID-based, so the
                        // directory is not practically enumerable.
                        .requestMatchers("/uploads/avatars/**").permitAll()
                        // Sprint 2 discovery stays behind Firebase auth so course browsing remains
                        // part of the verified learner flow defined in the current execution plan.
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().denyAll()
                )
                .addFilterBefore(firebaseTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        // Missing credentials are security errors outside MVC, so write the shared API error contract here.
                        .authenticationEntryPoint((request, response, authException) ->
                                apiErrorWriter.write(response, HttpStatus.UNAUTHORIZED, "Authentication required")
                        )
                        // Future role checks should also return the same contract instead of Spring's default HTML/error body.
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                apiErrorWriter.write(response, HttpStatus.FORBIDDEN, "Access denied")
                        )
                )
                .build();
    }
}

package com.edulife.security;

import com.edulife.common.error.ApiErrorWriter;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public FirebaseTokenFilter firebaseTokenFilter(FirebaseAuth firebaseAuth, ApiErrorWriter apiErrorWriter) {
        return new FirebaseTokenFilter(firebaseAuth, apiErrorWriter);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            FirebaseTokenFilter firebaseTokenFilter,
            ApiErrorWriter apiErrorWriter
    ) throws Exception {

        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
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

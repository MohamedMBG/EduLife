package com.edulife.security;

import com.google.firebase.auth.FirebaseAuth;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public FirebaseTokenFilter firebaseTokenFilter(FirebaseAuth firebaseAuth) {
        return new FirebaseTokenFilter(firebaseAuth);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            FirebaseTokenFilter firebaseTokenFilter
    ) throws Exception {

        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/public/**",
                                "/actuator/health"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(firebaseTokenFilter, UsernamePasswordAuthenticationFilter.class)
                /** By default, if no token is sent to a protected endpoint, Spring Security should return 403 sometimes depending on config. You want 401 **/
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required")
                        )
                )
                .build();
    }
}

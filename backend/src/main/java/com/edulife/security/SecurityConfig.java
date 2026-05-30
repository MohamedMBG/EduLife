package com.edulife.security;

import com.edulife.common.error.ApiErrorWriter;
import com.edulife.config.RateLimitFilter;
import com.edulife.users.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(CorsProperties.class)
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
    // RateLimitFilter is NOT @Component to avoid double-registration as a servlet filter.
    // It is instantiated here and added explicitly after FirebaseTokenFilter so that
    // SecurityContext is already populated when rate limit keys are resolved.
    public RateLimitFilter rateLimitFilter(ApiErrorWriter apiErrorWriter) {
        return new RateLimitFilter(apiErrorWriter);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        // Explicit allowlist only; never wildcard. An empty list means CORS is effectively closed
        // and any cross-origin preflight will be rejected by Spring's CorsProcessor with 403.
        config.setAllowedOrigins(properties.getAllowedOrigins());
        config.setAllowedMethods(properties.getAllowedMethods());
        config.setAllowedHeaders(properties.getAllowedHeaders());
        config.setMaxAge(properties.getMaxAgeSeconds());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            FirebaseTokenFilter firebaseTokenFilter,
            RateLimitFilter rateLimitFilter,
            ApiErrorWriter apiErrorWriter,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {

        return http
                // EduLife mobile clients use Bearer tokens only, so CSRF protection for cookie
                // sessions would add noise without improving the current threat model.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        // HSTS forces TLS once a client has connected over HTTPS; one year +
                        // includeSubDomains matches the standard secure-defaults profile.
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(Duration.ofDays(365).toSeconds())
                                // TLS terminates at the platform proxy so Spring sees plain HTTP;
                                // force HSTS regardless so the header reaches the browser.
                                .requestMatcher(AnyRequestMatcher.INSTANCE)
                        )
                        // Browsers should never sniff a Content-Type and frames must be forbidden
                        // to defend against clickjacking; Referer is hidden cross-origin so that
                        // path-based UUIDs do not leak via the Referer header.
                        .contentTypeOptions(c -> {})
                        .frameOptions(f -> f.deny())
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        // Avatar files are served as plain URLs to clients that may not carry the
                        // Firebase token (img tags, web cache). Filenames are UUID-based, so the
                        // directory is not practically enumerable.
                        .requestMatchers("/uploads/avatars/**").permitAll()
                        // Certificate verification is public so employers and institutions can
                        // confirm a certificate without a Firebase account.
                        .requestMatchers("/api/v1/certificates/verify/**").permitAll()
                        // Sprint 2 discovery stays behind Firebase auth so course browsing remains
                        // part of the verified learner flow defined in the current execution plan.
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().denyAll()
                )
                .addFilterBefore(firebaseTokenFilter, UsernamePasswordAuthenticationFilter.class)
                // Rate limiting runs after Firebase auth so the SecurityContext is populated
                // and user identity can be used as the bucket key instead of a raw IP address.
                .addFilterAfter(rateLimitFilter, FirebaseTokenFilter.class)
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

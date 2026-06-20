package com.edulife.security;

import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalised CORS allowlist so production never falls back to wildcard `*` origins.
 */
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    private static final List<String> FIRST_PARTY_ORIGINS =
            List.of("https://guided-journey-lab.vercel.app");

    private List<String> allowedOrigins = List.of();
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
    private List<String> allowedHeaders = List.of("Authorization", "Content-Type");
    private long maxAgeSeconds = 3600;

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public List<String> getEffectiveAllowedOrigins() {
        // The deployed web client is part of this application, so an old platform environment
        // override must not remove it and strand users after Firebase authentication succeeds.
        LinkedHashSet<String> effectiveOrigins = new LinkedHashSet<>(allowedOrigins);
        effectiveOrigins.addAll(FIRST_PARTY_ORIGINS);
        return List.copyOf(effectiveOrigins);
    }

    public List<String> getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(List<String> allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    public List<String> getAllowedHeaders() {
        return allowedHeaders;
    }

    public void setAllowedHeaders(List<String> allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
    }

    public long getMaxAgeSeconds() {
        return maxAgeSeconds;
    }

    public void setMaxAgeSeconds(long maxAgeSeconds) {
        this.maxAgeSeconds = maxAgeSeconds;
    }
}

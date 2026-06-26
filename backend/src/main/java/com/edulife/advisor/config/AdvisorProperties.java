package com.edulife.advisor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "edulife.advisor")
/** Externalized configuration properties for the advisor module (prefix {@code edulife.advisor}). */
public class AdvisorProperties {

    private String provider = "stub";
    private String model = "llama-3.3-70b-versatile";
    private String groqApiKey = "";
    private String groqBaseUrl = "https://api.groq.com/openai/v1";
    private int maxTokens = 1024;
    private int rateLimitPerHour = 10;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getGroqApiKey() { return groqApiKey; }
    public void setGroqApiKey(String groqApiKey) { this.groqApiKey = groqApiKey; }

    public String getGroqBaseUrl() { return groqBaseUrl; }
    public void setGroqBaseUrl(String groqBaseUrl) { this.groqBaseUrl = groqBaseUrl; }

    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }

    public int getRateLimitPerHour() { return rateLimitPerHour; }
    public void setRateLimitPerHour(int rateLimitPerHour) { this.rateLimitPerHour = rateLimitPerHour; }
}

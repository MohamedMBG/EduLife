package com.edulife.advisor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "edulife.advisor")
public class AdvisorProperties {

    private String provider = "stub";
    private String model = "none";
    private String groqApiKey = "";
    private int maxTokens = 600;
    // TODO: enforce via Bucket4j filter on /api/v1/advisor/recommend
    private int rateLimitPerHour = 10;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getGroqApiKey() { return groqApiKey; }
    public void setGroqApiKey(String groqApiKey) { this.groqApiKey = groqApiKey; }

    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }

    public int getRateLimitPerHour() { return rateLimitPerHour; }
    public void setRateLimitPerHour(int rateLimitPerHour) { this.rateLimitPerHour = rateLimitPerHour; }
}

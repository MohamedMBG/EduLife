package com.edulife.advisor.config;

import com.edulife.advisor.client.GroqLlmClient;
import com.edulife.advisor.client.LlmClient;
import com.edulife.advisor.client.StubLlmClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(AdvisorProperties.class)
public class AdvisorConfig {

    @Bean
    @ConditionalOnProperty(name = "edulife.advisor.provider", havingValue = "groq")
    public LlmClient groqLlmClient(RestClient.Builder builder, ObjectMapper objectMapper, AdvisorProperties properties) {
        return new GroqLlmClient(builder, objectMapper, properties);
    }

    @Bean
    @ConditionalOnMissingBean(LlmClient.class)
    public LlmClient stubLlmClient() {
        return new StubLlmClient();
    }
}

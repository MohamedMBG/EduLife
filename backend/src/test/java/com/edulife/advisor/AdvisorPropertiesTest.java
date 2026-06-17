package com.edulife.advisor;

import com.edulife.advisor.config.AdvisorProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AdvisorPropertiesTest {

    @Test
    void defaultsMatchSpec() {
        AdvisorProperties props = new AdvisorProperties();

        assertThat(props.getProvider()).isEqualTo("stub");
        assertThat(props.getModel()).isEqualTo("llama-3.3-70b-versatile");
        assertThat(props.getGroqApiKey()).isEqualTo("");
        assertThat(props.getGroqBaseUrl()).isEqualTo("https://api.groq.com/openai/v1");
        assertThat(props.getMaxTokens()).isEqualTo(1024);
        assertThat(props.getRateLimitPerHour()).isEqualTo(10);
    }

    @Test
    void bindsFromFlatProperties() {
        Map<String, String> source = Map.of(
                "edulife.advisor.provider", "groq",
                "edulife.advisor.model", "llama-3.3-70b-versatile",
                "edulife.advisor.groq-api-key", "gsk_test_key",
                "edulife.advisor.groq-base-url", "https://custom.groq.api/v1",
                "edulife.advisor.max-tokens", "800",
                "edulife.advisor.rate-limit-per-hour", "20"
        );

        AdvisorProperties props = new Binder(new MapConfigurationPropertySource(source))
                .bind("edulife.advisor", AdvisorProperties.class)
                .get();

        assertThat(props.getProvider()).isEqualTo("groq");
        assertThat(props.getModel()).isEqualTo("llama-3.3-70b-versatile");
        assertThat(props.getGroqApiKey()).isEqualTo("gsk_test_key");
        assertThat(props.getGroqBaseUrl()).isEqualTo("https://custom.groq.api/v1");
        assertThat(props.getMaxTokens()).isEqualTo(800);
        assertThat(props.getRateLimitPerHour()).isEqualTo(20);
    }

    @Test
    void groqApiKeyNotLeakedToDefaultValue() {
        AdvisorProperties props = new AdvisorProperties();
        assertThat(props.getGroqApiKey()).isBlank();
    }
}

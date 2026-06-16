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
        assertThat(props.getModel()).isEqualTo("none");
        assertThat(props.getGroqApiKey()).isEqualTo("");
        assertThat(props.getMaxTokens()).isEqualTo(600);
        assertThat(props.getRateLimitPerHour()).isEqualTo(10);
    }

    @Test
    void bindsFromFlatProperties() {
        Map<String, String> source = Map.of(
                "edulife.advisor.provider", "groq",
                "edulife.advisor.model", "llama-3.1-8b-instant",
                "edulife.advisor.groq-api-key", "gsk_test_key",
                "edulife.advisor.max-tokens", "800",
                "edulife.advisor.rate-limit-per-hour", "20"
        );

        AdvisorProperties props = new Binder(new MapConfigurationPropertySource(source))
                .bind("edulife.advisor", AdvisorProperties.class)
                .get();

        assertThat(props.getProvider()).isEqualTo("groq");
        assertThat(props.getModel()).isEqualTo("llama-3.1-8b-instant");
        assertThat(props.getGroqApiKey()).isEqualTo("gsk_test_key");
        assertThat(props.getMaxTokens()).isEqualTo(800);
        assertThat(props.getRateLimitPerHour()).isEqualTo(20);
    }

    @Test
    void groqApiKeyNotLeakedToDefaultValue() {
        // Guard: default must be blank so a missing env var never sends requests with an empty key
        AdvisorProperties props = new AdvisorProperties();
        assertThat(props.getGroqApiKey()).isBlank();
    }
}

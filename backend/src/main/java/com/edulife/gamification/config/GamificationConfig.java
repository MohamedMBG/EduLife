package com.edulife.gamification.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for gamification-specific beans.
 */
@Configuration
public class GamificationConfig {

    /**
     * Injecting a Clock keeps streak math testable — tests override this bean to
     * advance days deterministically instead of relying on Instant.now().
     */
    @Bean
    public Clock gamificationClock() {
        return Clock.systemUTC();
    }
}

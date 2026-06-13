package com.edulife.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registers auth-module configuration properties (staff role allowlist). */
@Configuration
@EnableConfigurationProperties(StaffRoleProperties.class)
public class AuthConfig {
}

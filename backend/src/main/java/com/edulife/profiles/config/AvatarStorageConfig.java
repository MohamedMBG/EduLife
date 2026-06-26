package com.edulife.profiles.config;

import java.nio.file.Paths;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers avatar storage properties and serves uploaded avatar files as static resources.
 */
@Configuration
@EnableConfigurationProperties(AvatarStorageProperties.class)
public class AvatarStorageConfig implements WebMvcConfigurer {

    private final AvatarStorageProperties properties;

    public AvatarStorageConfig(AvatarStorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve stored avatars from the configured directory so the URL returned by upload
        // is directly fetchable by Android and the web client without an extra signed-URL step.
        String absolute = Paths.get(properties.getStorageDir()).toAbsolutePath().normalize().toString();
        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations("file:" + absolute + "/");
    }
}

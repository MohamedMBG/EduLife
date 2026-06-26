package com.edulife.courses.config;

import java.nio.file.Paths;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configures static resource handling for locally stored course cover images.
 */
@Configuration
@EnableConfigurationProperties(CourseCoverStorageProperties.class)
public class CourseCoverStorageConfig implements WebMvcConfigurer {

    private final CourseCoverStorageProperties properties;

    public CourseCoverStorageConfig(CourseCoverStorageProperties properties) {
        this.properties = properties;
    }

    /** Maps {@code /uploads/course-covers/**} to the configured filesystem directory. */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolute = Paths.get(properties.getStorageDir()).toAbsolutePath().normalize().toString();
        registry.addResourceHandler("/uploads/course-covers/**")
                .addResourceLocations("file:" + absolute + "/");
    }
}

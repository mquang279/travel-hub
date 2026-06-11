package edu.uet.travel_hub.infrastructure.config;

import java.nio.file.Path;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
    private static final Path UPLOAD_ROOT = Path.of("uploads");

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadLocation = "file:" + UPLOAD_ROOT.toAbsolutePath().normalize() + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadLocation);
    }
}

package com.example.pandatribe.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("https://eve-helper.com","http://localhost:3000") // REMOVE "*" or multiple origins if using credentials
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("Authorization", "Content-Type") // Set required headers
                .exposedHeaders("Set-Cookie") // Expose cookies to frontend
                .allowCredentials(true);
    }
}

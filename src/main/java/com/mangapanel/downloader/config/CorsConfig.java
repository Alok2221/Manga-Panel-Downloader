package com.mangapanel.downloader.config;

import com.mangapanel.downloader.config.properties.CorsProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final CorsProperties props;

    public CorsConfig(CorsProperties props) {
        this.props = props;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = props.allowedOrigins() != null ? props.allowedOrigins() : List.of();
        List<String> methods = props.allowedMethods() != null ? props.allowedMethods() : List.of("GET", "POST", "DELETE", "PUT", "OPTIONS");
        List<String> headers = props.allowedHeaders() != null ? props.allowedHeaders() : List.of("*");

        registry.addMapping("/api/**")
                .allowedOrigins(origins.toArray(String[]::new))
                .allowedMethods(methods.toArray(String[]::new))
                .allowedHeaders(headers.toArray(String[]::new))
                .allowCredentials(props.allowCredentials());
    }
}


package com.mangapanel.downloader.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
        List<String> allowedOrigins,
        boolean allowCredentials,
        List<String> allowedMethods,
        List<String> allowedHeaders
) {
}


package com.mangapanel.downloader.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.http")
public record HttpClientProperties(
        int maxInMemoryBytes,
        String userAgent
) {
}


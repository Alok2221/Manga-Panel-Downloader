package com.mangapanel.downloader.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "translator")
public record TranslatorProperties(
        String baseUrl,
        Duration timeout
) {}

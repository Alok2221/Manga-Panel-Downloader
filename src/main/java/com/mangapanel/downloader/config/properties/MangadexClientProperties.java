package com.mangapanel.downloader.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "manga.source")
public record MangadexClientProperties(
        String downloadQuality,
        boolean forcePort443,
        Api api,
        Site site,
        Limits limits,
        Timeouts timeouts,
        Retry retry
) {
    public record Api(String baseUrl, String networkBaseUrl, String uploadBaseUrl) {}
    public record Site(String baseUrl) {}
    public record Limits(long rateLimitDelayMs, int maxFileSizeBytes) {}
    public record Timeouts(Duration httpTimeout, Duration atHomeReportTimeout) {}
    public record Retry(int attempts, Duration fixedDelay) {}
}


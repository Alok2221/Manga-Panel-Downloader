package com.mangapanel.downloader.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Getter
public class StorageConfig {

    @Value("${app.storage.path:./data/panels}")
    private String storagePath;

    private Path basePath;

    @PostConstruct
    public void init() throws IOException {
        basePath = Paths.get(storagePath).toAbsolutePath().normalize();
        Files.createDirectories(basePath);
    }

    public Path resolve(String... segments) {
        Path resolved = basePath;
        for (String segment : segments) {
            if (segment == null || segment.isEmpty() || segment.contains("..")) {
                throw new IllegalArgumentException("Invalid path segment: " + segment);
            }
            resolved = resolved.resolve(segment);
        }
        Path normalized = resolved.normalize();
        if (!normalized.startsWith(basePath)) {
            throw new IllegalArgumentException("Path traversal not allowed");
        }
        return normalized;
    }
}

package com.mangapanel.downloader.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .defaultHeader(HttpHeaders.USER_AGENT, "MangaPanelDownloader/1.0 (portfolio project)")
                .defaultHeader(HttpHeaders.ACCEPT, "application/json");
    }

    @Bean(name = "imageWebClient")
    public WebClient imageWebClient() {
        return WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .defaultHeader(HttpHeaders.USER_AGENT, "MangaPanelDownloader/1.0 (portfolio project)")
                .defaultHeader(HttpHeaders.ACCEPT, "*/*")
                .build();
    }
}

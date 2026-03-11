package com.mangapanel.downloader.config;

import com.mangapanel.downloader.config.properties.HttpClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder(HttpClientProperties props) {
        return WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(props.maxInMemoryBytes()))
                .defaultHeader(HttpHeaders.USER_AGENT, props.userAgent())
                .defaultHeader(HttpHeaders.ACCEPT, "application/json");
    }

    @Bean(name = "imageWebClient")
    public WebClient imageWebClient(HttpClientProperties props) {
        return WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(props.maxInMemoryBytes()))
                .defaultHeader(HttpHeaders.USER_AGENT, props.userAgent())
                .defaultHeader(HttpHeaders.ACCEPT, "*/*")
                .build();
    }
}

package com.mangapanel.downloader.integration.translator;

import com.mangapanel.downloader.config.properties.TranslatorProperties;
import com.mangapanel.downloader.integration.translator.dto.*;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TranslatorClientTest {

    private MockWebServer server;
    private TranslatorClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        TranslatorProperties props = new TranslatorProperties(server.url("/").toString(), Duration.ofSeconds(5));
        WebClient.Builder builder = WebClient.builder();
        client = new TranslatorClient(builder, props);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void performOcr_sendsRequestAndReturnsSegments() {
        String body = """
                {"segments":[{"panel_id":1,"sequence_index":0,"text":"Hello world"}]}
                """;
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(body));

        PanelImageDto panel = PanelImageDto.builder().id(1L).imageBase64("YWJj").build();
        OcrResponseDto result = client.performOcr(1L, List.of(panel), "en");

        assertThat(result).isNotNull();
        assertThat(result.getSegments()).hasSize(1);
        assertThat(result.getSegments().get(0).getPanelId()).isEqualTo(1L);
        assertThat(result.getSegments().get(0).getText()).isEqualTo("Hello world");
    }

    @Test
    void translateSegments_sendsRequestAndReturnsTranslations() {
        String body = """
                {"segments":[{"id":10,"translated_text":"Cześć"}]}
                """;
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(body));

        TranslateSegmentInDto in = TranslateSegmentInDto.builder().id(10L).panelId(1L).sourceText("Hi").build();
        TranslateRequestDto request = TranslateRequestDto.builder()
                .segments(List.of(in))
                .sourceLanguage("en")
                .targetLanguage("pl")
                .build();

        TranslateResponseDto result = client.translateSegments(request);

        assertThat(result).isNotNull();
        assertThat(result.getSegments()).hasSize(1);
        assertThat(result.getSegments().get(0).getId()).isEqualTo(10L);
        assertThat(result.getSegments().get(0).getTranslatedText()).isEqualTo("Cześć");
    }

    @Test
    void performOcr_throwsWhenServerReturnsError() {
        server.enqueue(new MockResponse().setResponseCode(502).setBody("Bad Gateway"));
        PanelImageDto panel = PanelImageDto.builder().id(1L).imageBase64("x").build();

        assertThatThrownBy(() -> client.performOcr(1L, List.of(panel), "en"))
                .isInstanceOf(TranslatorClientException.class)
                .hasMessageContaining("502");
    }

    @Test
    void performOcr_throwsWhenPanelsEmpty() {
        assertThatThrownBy(() -> client.performOcr(1L, List.of(), "en"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("panels");
    }

    @Test
    void translateSegments_throwsWhenSegmentsEmpty() {
        TranslateRequestDto request = TranslateRequestDto.builder().segments(List.of()).build();
        assertThatThrownBy(() -> client.translateSegments(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("segments");
    }
}

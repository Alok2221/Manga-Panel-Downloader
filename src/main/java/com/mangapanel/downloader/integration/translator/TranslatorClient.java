package com.mangapanel.downloader.integration.translator;

import com.mangapanel.downloader.config.properties.TranslatorProperties;
import com.mangapanel.downloader.integration.translator.dto.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TranslatorClient {

    private static final Logger log = LoggerFactory.getLogger(TranslatorClient.class);

    private final WebClient.Builder webClientBuilder;
    private final TranslatorProperties props;

    public OcrResponseDto performOcr(Long chapterId, List<PanelImageDto> panels, String sourceLang) {
        if (panels == null || panels.isEmpty()) {
            throw new IllegalArgumentException("panels must not be empty");
        }
        OcrRequestDto request = OcrRequestDto.builder()
                .chapterId(chapterId)
                .panels(panels)
                .sourceLanguage(sourceLang != null ? sourceLang : "en")
                .build();
        return post("/ocr", request, OcrResponseDto.class);
    }

    public TranslateResponseDto translateSegments(TranslateRequestDto request) {
        if (request == null || request.getSegments() == null || request.getSegments().isEmpty()) {
            throw new IllegalArgumentException("segments must not be empty");
        }
        return post("/translate", request, TranslateResponseDto.class);
    }

    private <Req, Res> Res post(String path, Req body, Class<Res> responseType) {
        WebClient client = webClientBuilder
                .clone()
                .baseUrl(props.baseUrl())
                .build();

        return client.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                        .flatMap(responseBody -> {
                            String msg = "Translator service error: " + resp.statusCode() + " – " + responseBody;
                            log.warn("{}", msg);
                            return Mono.error(new TranslatorClientException(msg, responseBody));
                        }))
                .bodyToMono(responseType)
                .timeout(props.timeout())
                .block();
    }
}

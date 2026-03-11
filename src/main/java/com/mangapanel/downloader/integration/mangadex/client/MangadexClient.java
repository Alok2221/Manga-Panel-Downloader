package com.mangapanel.downloader.integration.mangadex.client;

import com.mangapanel.downloader.config.properties.MangadexClientProperties;
import com.mangapanel.downloader.integration.MangaSourceClient;
import com.mangapanel.downloader.integration.mangadex.dto.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class MangadexClient implements MangaSourceClient {

    private static final Logger log = LoggerFactory.getLogger(MangadexClient.class);
    private static final Pattern CHAPTER_URL_PATTERN = Pattern.compile(
            "(?:https?://)?(www\\.)?mangadex\\.org/chapter/([a-f0-9-]{36})?",
            Pattern.CASE_INSENSITIVE
    );
    private final WebClient.Builder webClientBuilder;
    private final MangadexClientProperties props;

    @Override
    public String extractChapterIdFromUrl(String chapterUrl) {
        if (chapterUrl == null) return null;
        Matcher m = CHAPTER_URL_PATTERN.matcher(chapterUrl.trim());
        return m.find() ? m.group(2) : null;
    }

    private static final String MANGA_FEED_SUFFIX = "/feed";

    @Override
    public Mono<MangadexChapterResponse> fetchChapter(String chapterId) {
        WebClient client = webClientBuilder.build();
        return client.get()
                .uri(props.api().baseUrl() + "/chapter/{id}", chapterId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                        .flatMap(body -> Mono.error(new MangadexClientException("Chapter not found or unavailable: " + body))))
                .bodyToMono(MangadexChapterResponse.class)
                .timeout(props.timeouts().httpTimeout())
                .retryWhen(Retry.fixedDelay(Math.max(0, props.retry().attempts()), props.retry().fixedDelay())
                        .filter(ex -> ex instanceof WebClientResponseException.NotFound)
                        .doBeforeRetry(s -> log.warn("Retrying chapter fetch for {}", chapterId)));
    }

    @Override
    public Mono<MangadexMangaResponse> fetchManga(String mangaId) {
        WebClient client = webClientBuilder.build();
        return client.get()
                .uri(props.api().baseUrl() + "/manga/{id}", mangaId)
                .retrieve()
                .bodyToMono(MangadexMangaResponse.class)
                .timeout(props.timeouts().httpTimeout());
    }

    @Override
    public Mono<MangadexGroupResponse> fetchGroup(String groupId) {
        WebClient client = webClientBuilder.build();
        return client.get()
                .uri(props.api().baseUrl() + "/group/{id}", groupId)
                .retrieve()
                .bodyToMono(MangadexGroupResponse.class)
                .timeout(props.timeouts().httpTimeout());
    }

    @Override
    public Mono<AtHomeResponse> fetchAtHomeServer(String chapterId) {
        WebClient client = webClientBuilder.build();
        String uri = props.api().baseUrl() + "/at-home/server/" + chapterId + (props.forcePort443() ? "?forcePort443=true" : "");
        return client.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(AtHomeResponse.class)
                .timeout(props.timeouts().httpTimeout());
    }

    @Override
    public void reportAtHomeLoadResult(String imageUrl, boolean success, boolean cached, int bytes, int durationMs) {
        if (imageUrl == null || imageUrl.contains("mangadex.org")) {
            return;
        }
        try {
            var body = Map.of(
                    "url", imageUrl,
                    "success", success,
                    "cached", cached,
                    "bytes", bytes,
                    "duration", durationMs
            );
            webClientBuilder.build()
                    .post()
                    .uri(props.api().networkBaseUrl() + "/report")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(props.timeouts().atHomeReportTimeout())
                    .subscribe(v -> {
                    }, e -> log.debug("MangaDex@Home report failed: {}", e.getMessage()));
        } catch (Exception e) {
            log.debug("MangaDex@Home report error: {}", e.getMessage());
        }
    }

    @Override
    public String buildImageUrl(String baseUrl, String chapterHash, String filename) {
        String qualityPath = "data-saver".equals(props.downloadQuality()) ? "data-saver" : "data";
        String base = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        return base + qualityPath + "/" + chapterHash + "/" + filename;
    }

    @Override
    public List<String> getFilenames(AtHomeResponse atHome) {
        if (atHome == null || atHome.getChapter() == null) return List.of();
        if ("data-saver".equals(props.downloadQuality())) {
            List<String> saver = atHome.getChapter().getDataSaverFilenames();
            return saver != null ? saver : atHome.getChapter().getDataFilenames();
        }
        List<String> data = atHome.getChapter().getDataFilenames();
        return data != null ? data : atHome.getChapter().getDataSaverFilenames();
    }

    @Override
    public long getRateLimitDelayMs() {
        return props.limits().rateLimitDelayMs();
    }

    @Override
    public int getHttpTimeoutSeconds() {
        return (int) Math.max(1, props.timeouts().httpTimeout().toSeconds());
    }

    @Override
    public String getUploadBaseUrl() {
        return props.api().uploadBaseUrl();
    }

    @Override
    public int getMaxFileSizeBytes() {
        return props.limits().maxFileSizeBytes();
    }

    @Override
    public int getRetryAttempts() {
        return Math.max(1, props.retry().attempts());
    }

    @Override
    public String buildChapterPageUrl(String chapterId) {
        String base = props.site().baseUrl();
        if (base == null || base.isBlank()) base = "https://mangadex.org";
        String trimmed = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return trimmed + "/chapter/" + chapterId;
    }

    /**
     * Search manga by title via MangaDex GET /manga.
     */
    @Override
    public Mono<MangadexMangaListResponse> searchManga(String title, int limit, int offset) {
        WebClient client = webClientBuilder.build();
        String encodedTitle = URLEncoder.encode(title != null ? title : "", StandardCharsets.UTF_8);
        String uri = props.api().baseUrl() + "/manga?title=" + encodedTitle + "&limit=" + limit + "&offset=" + offset + "&contentRating[]=safe&contentRating[]=suggestive";
        return client.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(MangadexMangaListResponse.class)
                .timeout(props.timeouts().httpTimeout());
    }

    @Override
    public Mono<MangadexChapterFeedResponse> getMangaFeed(String mangaId, int limit, int offset, String translatedLanguage) {
        WebClient client = webClientBuilder.build();
        StringBuilder sb = new StringBuilder(props.api().baseUrl()).append("/manga/").append(mangaId).append(MANGA_FEED_SUFFIX)
                .append("?limit=").append(limit)
                .append("&offset=").append(offset)
                .append("&order[chapter]=asc");
        if (translatedLanguage != null && !translatedLanguage.isBlank()) {
            sb.append("&translatedLanguage[]=").append(URLEncoder.encode(translatedLanguage, StandardCharsets.UTF_8));
        }
        return client.get()
                .uri(sb.toString())
                .retrieve()
                .bodyToMono(MangadexChapterFeedResponse.class)
                .timeout(props.timeouts().httpTimeout());
    }

    @Override
    public Mono<MangadexCoverListResponse> fetchCoversForManga(String mangaId) {
        WebClient client = webClientBuilder.build();
        String uri = props.api().baseUrl() + "/cover?manga=" + mangaId + "&limit=1&order[volume]=desc";
        return client.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(MangadexCoverListResponse.class)
                .timeout(props.timeouts().httpTimeout());
    }

    public static class MangadexClientException extends RuntimeException {
        public MangadexClientException(String message) {
            super(message);
        }
    }
}


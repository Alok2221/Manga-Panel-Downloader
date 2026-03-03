package com.mangapanel.downloader.client;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import com.mangapanel.downloader.client.dto.MangadexChapterFeedResponse;
import com.mangapanel.downloader.client.dto.MangadexMangaListResponse;

import java.time.Duration;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class MangaSourceClient {

    private static final Logger log = LoggerFactory.getLogger(MangaSourceClient.class);
    private static final Pattern CHAPTER_URL_PATTERN = Pattern.compile(
            "(?:https?://)?(www\\.)?mangadex\\.org/chapter/([a-f0-9-]{36})(?:/)?",
            Pattern.CASE_INSENSITIVE
    );
    private static final String CHAPTER_API = "https://api.mangadex.org/chapter";
    private static final String AT_HOME_API = "https://api.mangadex.org/at-home/server";
    private static final String AT_HOME_REPORT_API = "https://api.mangadex.network/report";
    private static final long RATE_LIMIT_DELAY_MS = 210;
    private static final int MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024;
    private static final int HTTP_TIMEOUT_SECONDS = 10;

    private final WebClient.Builder webClientBuilder;

    @Value("${manga.source.download.quality:data}")
    private String quality;

    @Value("${manga.source.force-port-443:false}")
    private boolean forcePort443;

    public String extractChapterIdFromUrl(String chapterUrl) {
        if (chapterUrl == null) return null;
        Matcher m = CHAPTER_URL_PATTERN.matcher(chapterUrl.trim());
        return m.find() ? m.group(2) : null;
    }

    private static final String MANGA_API = "https://api.mangadex.org/manga";
    private static final String MANGA_FEED_SUFFIX = "/feed";

    public Mono<MangaSourceApiResponse> fetchChapter(String chapterId) {
        WebClient client = webClientBuilder.build();
        return client.get()
                .uri(CHAPTER_API + "/{id}", chapterId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                        .flatMap(body -> Mono.error(new MangaSourceException("Chapter not found or unavailable: " + body))))
                .bodyToMono(MangaSourceApiResponse.class)
                .timeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
                .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(1))
                        .filter(ex -> ex instanceof WebClientResponseException.NotFound)
                        .doBeforeRetry(s -> log.warn("Retrying chapter fetch for {}", chapterId)));
    }

    public Mono<MangaSourceMangaResponse> fetchManga(String mangaId) {
        WebClient client = webClientBuilder.build();
        return client.get()
                .uri(MANGA_API + "/{id}", mangaId)
                .retrieve()
                .bodyToMono(MangaSourceMangaResponse.class)
                .timeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS));
    }

    public Mono<AtHomeResponse> fetchAtHomeServer(String chapterId) {
        WebClient client = webClientBuilder.build();
        String uri = AT_HOME_API + "/" + chapterId + (forcePort443 ? "?forcePort443=true" : "");
        return client.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(AtHomeResponse.class)
                .timeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS));
    }

    public void reportAtHomeLoadResult(String imageUrl, boolean success, boolean cached, int bytes, int durationMs) {
        if (imageUrl == null || imageUrl.contains("mangadex.org")) {
            return;
        }
        try {
            var body = java.util.Map.of(
                    "url", imageUrl,
                    "success", success,
                    "cached", cached,
                    "bytes", bytes,
                    "duration", durationMs
            );
            webClientBuilder.build()
                    .post()
                    .uri(AT_HOME_REPORT_API)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(5))
                    .subscribe(
                            v -> {
                            },
                            e -> log.debug("MangaDex@Home report failed: {}", e.getMessage())
                    );
        } catch (Exception e) {
            log.debug("MangaDex@Home report error: {}", e.getMessage());
        }
    }

    public String buildImageUrl(String baseUrl, String chapterHash, String filename) {
        String qualityPath = "data-saver".equals(quality) ? "data-saver" : "data";
        String base = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        return base + qualityPath + "/" + chapterHash + "/" + filename;
    }

    public List<String> getFilenames(AtHomeResponse atHome) {
        if (atHome == null || atHome.getChapter() == null) return List.of();
        if ("data-saver".equals(quality)) {
            List<String> saver = atHome.getChapter().getDataSaverFilenames();
            return saver != null ? saver : atHome.getChapter().getDataFilenames();
        }
        List<String> data = atHome.getChapter().getDataFilenames();
        return data != null ? data : atHome.getChapter().getDataSaverFilenames();
    }

    public long getRateLimitDelayMs() {
        return RATE_LIMIT_DELAY_MS;
    }

    public int getHttpTimeoutSeconds() {
        return HTTP_TIMEOUT_SECONDS;
    }

    /**
     * Search manga by title via MangaDex GET /manga.
     * @param title search query (title)
     * @param limit max results (default 20)
     * @param offset pagination offset
     */
    public Mono<MangadexMangaListResponse> searchManga(String title, int limit, int offset) {
        WebClient client = webClientBuilder.build();
        String encodedTitle = URLEncoder.encode(title != null ? title : "", StandardCharsets.UTF_8);
        String uri = MANGA_API + "?title=" + encodedTitle + "&limit=" + limit + "&offset=" + offset + "&contentRating[]=safe&contentRating[]=suggestive";
        return client.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(MangadexMangaListResponse.class)
                .timeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS));
    }

    /**
     * Get chapter feed for a manga via MangaDex GET /manga/{id}/feed.
     * @param mangaId MangaDex manga UUID
     * @param limit max chapters (default 96)
     * @param offset pagination offset
     * @param translatedLanguage optional language filter (e.g. "en")
     */
    public Mono<MangadexChapterFeedResponse> getMangaFeed(String mangaId, int limit, int offset, String translatedLanguage) {
        WebClient client = webClientBuilder.build();
        StringBuilder sb = new StringBuilder(MANGA_API).append("/").append(mangaId).append(MANGA_FEED_SUFFIX)
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
                .timeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS));
    }

    public static class MangaSourceException extends RuntimeException {
        public MangaSourceException(String message) {
            super(message);
        }
    }
}

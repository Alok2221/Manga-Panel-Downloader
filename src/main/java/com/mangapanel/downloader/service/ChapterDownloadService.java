package com.mangapanel.downloader.service;

import com.mangapanel.downloader.entity.Chapter;
import com.mangapanel.downloader.entity.Manga;
import com.mangapanel.downloader.entity.Panel;
import com.mangapanel.downloader.integration.MangaSourceClient;
import com.mangapanel.downloader.integration.mangadex.dto.AtHomeResponse;
import com.mangapanel.downloader.integration.mangadex.dto.MangadexChapterResponse;
import com.mangapanel.downloader.integration.mangadex.dto.MangadexMangaResponse;
import com.mangapanel.downloader.repository.ChapterRepository;
import com.mangapanel.downloader.repository.MangaRepository;
import com.mangapanel.downloader.repository.PanelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChapterDownloadService {

    private final MangaSourceClient mangaSourceClient;
    private final MangaRepository mangaRepository;
    private final ChapterRepository chapterRepository;
    private final PanelRepository panelRepository;
    private final WebClient imageWebClient;

    @Transactional
    public Chapter createChapterAndStartDownload(String chapterUrl) {
        String url = chapterUrl == null ? null : chapterUrl.trim();
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Chapter URL is required");
        }
        if (chapterRepository.existsByUrl(url)) {
            throw new IllegalArgumentException("Chapter already downloaded: " + url);
        }
        String chapterId = mangaSourceClient.extractChapterIdFromUrl(url);
        if (chapterId == null) {
            throw new IllegalArgumentException("URL must be a MangaDex chapter URL (https://mangadex.org/chapter/...)");
        }
        log.info("Fetching MangaDex chapter metadata: {}", chapterId);
        MangadexChapterResponse chapterResp = mangaSourceClient.fetchChapter(chapterId).block();
        if (chapterResp == null || chapterResp.getData() == null || chapterResp.getData().getAttributes() == null) {
            throw new IllegalStateException("Chapter not found or invalid response from MangaDex");
        }
        MangadexChapterResponse.ChapterData data = chapterResp.getData();
        String mangaId = data.getRelationships() == null ? null : data.getRelationships().stream()
                .filter(r -> "manga".equals(r.getType()))
                .map(MangadexChapterResponse.MangadexRelationship::getId)
                .findFirst()
                .orElse(null);
        String scanlationGroupId = data.getRelationships() == null ? null : data.getRelationships().stream()
                .filter(r -> "scanlation_group".equals(r.getType()))
                .map(MangadexChapterResponse.MangadexRelationship::getId)
                .findFirst()
                .orElse(null);

        String mangaTitle = "Unknown";
        if (mangaId != null) {
            try {
                MangadexMangaResponse mangaResp = mangaSourceClient.fetchManga(mangaId).block();
                if (mangaResp != null && mangaResp.getData() != null && mangaResp.getData().getAttributes() != null
                        && mangaResp.getData().getAttributes().getTitle() != null
                        && !mangaResp.getData().getAttributes().getTitle().isEmpty()) {
                    mangaTitle = mangaResp.getData().getAttributes().getTitle().getOrDefault("en",
                            mangaResp.getData().getAttributes().getTitle().values().iterator().next());
                }
            } catch (Exception e) {
                log.warn("Could not fetch manga title for {}: {}", mangaId, e.getMessage());
            }
        }

        BigDecimal chapterNum = null;
        try {
            if (data.getAttributes().getChapter() != null && !data.getAttributes().getChapter().isEmpty()) {
                chapterNum = new BigDecimal(data.getAttributes().getChapter());
            }
        } catch (Exception ignored) {
        }

        String chapterTitle = data.getAttributes().getTitle();

        Manga manga = Manga.builder()
                .title(mangaTitle)
                .createdAt(Instant.now())
                .build();
        if (mangaId != null) {
            try {
                var coverResp = mangaSourceClient.fetchCoversForManga(mangaId).block();
                if (coverResp != null
                        && coverResp.getData() != null
                        && !coverResp.getData().isEmpty()
                        && coverResp.getData().get(0).getAttributes() != null) {
                    String fileName = coverResp.getData().get(0).getAttributes().getFileName();
                    if (fileName != null && !fileName.isBlank()) {
                        String baseUploads = mangaSourceClient.getUploadBaseUrl();
                        String base = baseUploads.endsWith("/") ? baseUploads.substring(0, baseUploads.length() - 1) : baseUploads;
                        String coverUrl = base + "/covers/" + mangaId + "/" + fileName + ".256.jpg";
                        manga.setCoverUrl(coverUrl);
                    }
                }
            } catch (Exception e) {
                log.warn("Could not fetch cover for manga {}: {}", mangaId, e.getMessage());
            }
        }
        manga = mangaRepository.save(manga);
        String volume = data.getAttributes().getVolume();
        Chapter chapter = Chapter.builder()
                .manga(manga)
                .chapterNumber(chapterNum)
                .title(chapterTitle != null ? chapterTitle : "Ch. " + data.getAttributes().getChapter())
                .url(url)
                .totalPanels(0)
                .downloadedAt(Instant.now())
                .language(data.getAttributes().getTranslatedLanguage())
                .sourceChapterId(chapterId)
                .volume(volume != null && !volume.isBlank() ? volume : null)
                .build();
        if (scanlationGroupId != null) {
            try {
                var groupResp = mangaSourceClient.fetchGroup(scanlationGroupId).block();
                if (groupResp != null
                        && groupResp.getData() != null
                        && groupResp.getData().getAttributes() != null) {
                    chapter.setScanlationGroup(groupResp.getData().getAttributes().getName());
                }
            } catch (Exception e) {
                log.warn("Could not fetch scanlation group {}: {}", scanlationGroupId, e.getMessage());
            }
        }
        chapter = chapterRepository.save(chapter);
        log.info("Created MangaDex chapter id={}, panels to download will be resolved via at-home", chapter.getId());
        return chapter;
    }

    @Async("downloadExecutor")
    public void startPanelDownloadAsync(Long chapterId) {
        var chapterOpt = chapterRepository.findById(chapterId);
        if (chapterOpt.isEmpty()) {
            log.error("Chapter id={} not found", chapterId);
            return;
        }
        Chapter chapter = chapterOpt.get();
        if (chapter.getSourceChapterId() == null) {
            log.error("Chapter {} has no MangaDex sourceChapterId", chapterId);
            return;
        }
        downloadMangaDexChapter(chapter);
    }

    private void downloadMangaDexChapter(Chapter chapter) {
        String chapterId = chapter.getSourceChapterId();
        AtHomeResponse atHome = mangaSourceClient.fetchAtHomeServer(chapterId).block();
        if (atHome == null || atHome.getBaseUrl() == null || atHome.getChapter() == null) {
            log.error("MangaDex at-home failed for chapter {}", chapter.getId());
            return;
        }
        List<String> filenames = mangaSourceClient.getFilenames(atHome);
        if (filenames.isEmpty()) {
            log.warn("No filenames from at-home for chapter {}", chapter.getId());
            return;
        }
        chapter.setTotalPanels(filenames.size());
        chapterRepository.saveAndFlush(chapter);
        String baseUrl = atHome.getBaseUrl().endsWith("/") ? atHome.getBaseUrl() : atHome.getBaseUrl() + "/";
        String hash = atHome.getChapter().getHash();
        int pageNumber = 1;
        for (String filename : filenames) {
            String imageUrl = mangaSourceClient.buildImageUrl(baseUrl, hash, filename);
            boolean ok = fetchAndSaveMangaDexPanel(chapter, filename, imageUrl, pageNumber);
            if (ok) pageNumber++;
            try {
                TimeUnit.MILLISECONDS.sleep(mangaSourceClient.getRateLimitDelayMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        log.info("Finished MangaDex panel download for chapter {}: {} panels", chapter.getId(), pageNumber - 1);
    }

    private boolean fetchAndSaveMangaDexPanel(Chapter chapter, String filename, String imageUrl, int pageNumber) {
        int timeoutSeconds = mangaSourceClient.getHttpTimeoutSeconds();
        int maxRetries = Math.max(1, mangaSourceClient.getRetryAttempts());
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            long start = System.currentTimeMillis();
            int bytesCount = 0;
            boolean success = false;
            boolean cached = false;
            try {
                ResponseEntity<byte[]> entity = imageWebClient.get()
                        .uri(imageUrl)
                        .retrieve()
                        .toEntity(byte[].class)
                        .block(java.time.Duration.ofSeconds(timeoutSeconds));
                if (entity != null && entity.getBody() != null) {
                    String xCache = entity.getHeaders().getFirst("X-Cache");
                    cached = xCache != null && xCache.toUpperCase().startsWith("HIT");
                    byte[] bytes = entity.getBody();
                    if (bytes != null && bytes.length > 0) {
                        bytesCount = bytes.length;
                        if (bytes.length > mangaSourceClient.getMaxFileSizeBytes()) {
                            log.warn("Image too large ({} bytes), skipping: {}", bytes.length, imageUrl);
                            return false;
                        }
                        String format = filename.contains(".") ? filename.substring(filename.lastIndexOf('.') + 1) : "jpg";
                        Panel panel = Panel.builder()
                                .chapter(chapter)
                                .pageNumber(pageNumber)
                                .imageUrl(imageUrl)
                                .localPath(null)
                                .data(bytes)
                                .fileSize((long) bytes.length)
                                .format(format)
                                .build();
                        panelRepository.save(panel);
                        log.debug("Saved panel {}/{} for chapter {}", pageNumber, chapter.getTotalPanels(), chapter.getId());
                        success = true;
                        return true;
                    }
                }
            } catch (WebClientResponseException e) {
                int status = e.getStatusCode().value();
                log.warn("Attempt {} failed for {}: HTTP {}", attempt + 1, imageUrl, status);
                if (attempt == maxRetries - 1) {
                    log.error("Giving up after {} attempts for {}", maxRetries, imageUrl);
                    throw new RuntimeException("Failed to download image: HTTP " + status, e);
                }
            } catch (Exception e) {
                log.warn("Attempt {} failed for {}: {}", attempt + 1, imageUrl, e.getMessage());
                if (attempt == maxRetries - 1) throw new RuntimeException(e);
            } finally {
                int durationMs = (int) (System.currentTimeMillis() - start);
                mangaSourceClient.reportAtHomeLoadResult(imageUrl, success, cached, bytesCount, durationMs);
            }
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
        return false;
    }
}

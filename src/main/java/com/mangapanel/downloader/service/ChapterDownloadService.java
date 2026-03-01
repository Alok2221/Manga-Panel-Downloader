package com.mangapanel.downloader.service;

import com.mangapanel.downloader.client.AtHomeResponse;
import com.mangapanel.downloader.client.GlobalComixClient;
import com.mangapanel.downloader.client.MangaSourceClient;
import com.mangapanel.downloader.config.StorageConfig;
import com.mangapanel.downloader.dto.ChapterFetchResult;
import com.mangapanel.downloader.entity.Chapter;
import com.mangapanel.downloader.entity.Manga;
import com.mangapanel.downloader.entity.Panel;
import com.mangapanel.downloader.repository.ChapterRepository;
import com.mangapanel.downloader.repository.MangaRepository;
import com.mangapanel.downloader.source.ChapterSourceProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChapterDownloadService {

    private static final int MAX_RETRIES = 3;
    private static final int MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024;

    private final List<ChapterSourceProvider> chapterSourceProviders;
    private final MangaSourceClient mangaSourceClient;
    private final GlobalComixClient globalComixClient;
    private final MangaRepository mangaRepository;
    private final ChapterRepository chapterRepository;
    private final StorageConfig storageConfig;
    private final WebClient imageWebClient;

    private final ConcurrentHashMap<Long, List<String>> pendingImageUrls = new ConcurrentHashMap<>();

    @Transactional
    public Chapter createChapterAndStartDownload(String chapterUrl) {
        String url = chapterUrl == null ? null : chapterUrl.trim();
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Chapter URL is required");
        }
        ChapterSourceProvider provider = chapterSourceProviders.stream()
                .filter(p -> p.supports(url))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported URL. Use a chapter URL from MangaDex, GlobalComix, or MangaPlus."));
        if (chapterRepository.existsByUrl(url)) {
            throw new IllegalArgumentException("Chapter already downloaded: " + url);
        }
        log.info("Fetching chapter from {}: {}", provider.getSourceId(), url);
        ChapterFetchResult result = provider.fetchChapter(url);
        Manga manga = mangaRepository.save(Manga.builder()
                .title(result.getMangaTitle() != null ? result.getMangaTitle() : provider.getSourceId())
                .createdAt(Instant.now())
                .build());
        Integer totalPanels = result.getImageUrls() != null ? result.getImageUrls().size() : null;
        Chapter chapter = Chapter.builder()
                .manga(manga)
                .chapterNumber(result.getChapterNumber())
                .title(result.getChapterTitle())
                .url(url)
                .totalPanels(totalPanels != null ? totalPanels : 0)
                .downloadedAt(Instant.now())
                .language(result.getLanguage())
                .sourceChapterId(result.getSourceChapterId())
                .build();
        chapter = chapterRepository.save(chapter);
        if (result.getImageUrls() != null && !result.getImageUrls().isEmpty()) {
            pendingImageUrls.put(chapter.getId(), result.getImageUrls());
        }
        if (result.getSourceChapterId() != null && (totalPanels == null || totalPanels == 0)) {
            chapter.setTotalPanels(0);
            chapterRepository.save(chapter);
        }
        log.info("Created chapter id={}, source={}, panels to download={}", chapter.getId(), provider.getSourceId(),
                result.getImageUrls() != null ? result.getImageUrls().size() : "(at-home)");
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
        if (chapter.getSourceChapterId() != null) {
            downloadMangaDexChapter(chapter);
        } else {
            List<String> imageUrls = pendingImageUrls.remove(chapterId);
            if (imageUrls == null || imageUrls.isEmpty()) {
                ChapterSourceProvider provider = chapterSourceProviders.stream()
                        .filter(p -> p.supports(chapter.getUrl()))
                        .findFirst()
                        .orElse(null);
                if (provider != null) {
                    try {
                        ChapterFetchResult result = provider.fetchChapter(chapter.getUrl());
                        imageUrls = result.getImageUrls();
                    } catch (Exception e) {
                        log.error("Re-fetch failed for chapter {}: {}", chapterId, e.getMessage());
                        return;
                    }
                }
            }
            if (imageUrls == null || imageUrls.isEmpty()) {
                log.warn("No image URLs for chapter {}", chapterId);
                return;
            }
            downloadAllPanelsFromUrls(chapter, imageUrls);
        }
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
        String dirName = "ch_" + chapter.getId();
        try {
            Files.createDirectories(storageConfig.resolve(dirName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int pageNumber = 1;
        for (String filename : filenames) {
            String imageUrl = mangaSourceClient.buildImageUrl(baseUrl, hash, filename);
            boolean ok = fetchAndSaveMangaDexPanel(chapter, dirName, filename, imageUrl, pageNumber);
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

    private void downloadAllPanelsFromUrls(Chapter chapter, List<String> imageUrls) {
        String dirName = "ch_" + chapter.getId();
        try {
            Files.createDirectories(storageConfig.resolve(dirName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int pageNumber = 1;
        for (String imageUrl : imageUrls) {
            String filename = globalComixClient.filenameForPage(pageNumber, imageUrl);
            boolean ok = fetchAndSavePanel(chapter, dirName, filename, imageUrl, pageNumber, globalComixClient.getHttpTimeoutSeconds());
            if (ok) pageNumber++;
            try {
                TimeUnit.MILLISECONDS.sleep(globalComixClient.getRateLimitMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        log.info("Finished panel download for chapter {}: {} panels saved", chapter.getId(), pageNumber - 1);
    }

    private boolean fetchAndSaveMangaDexPanel(Chapter chapter, String dirName, String filename, String imageUrl, int pageNumber) {
        int timeoutSeconds = mangaSourceClient.getHttpTimeoutSeconds();
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
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
                        if (bytes.length > MAX_FILE_SIZE_BYTES) {
                            log.warn("Image too large ({} bytes), skipping: {}", bytes.length, imageUrl);
                            return false;
                        }
                        java.nio.file.Path path = storageConfig.resolve(dirName, filename);
                        Files.write(path, bytes);
                        String format = filename.contains(".") ? filename.substring(filename.lastIndexOf('.') + 1) : "jpg";
                        Panel panel = Panel.builder()
                                .chapter(chapter)
                                .pageNumber(pageNumber)
                                .imageUrl(imageUrl)
                                .localPath(path.toString())
                                .fileSize((long) bytes.length)
                                .format(format)
                                .build();
                        chapter.getPanels().add(panel);
                        chapterRepository.saveAndFlush(chapter);
                        log.debug("Saved panel {}/{} for chapter {}", pageNumber, chapter.getTotalPanels(), chapter.getId());
                        success = true;
                        return true;
                    }
                }
            } catch (WebClientResponseException e) {
                int status = e.getStatusCode().value();
                log.warn("Attempt {} failed for {}: HTTP {}", attempt + 1, imageUrl, status);
                if (attempt == MAX_RETRIES - 1) {
                    log.error("Giving up after {} attempts for {}", MAX_RETRIES, imageUrl);
                    throw new RuntimeException("Failed to download image: HTTP " + status, e);
                }
            } catch (Exception e) {
                log.warn("Attempt {} failed for {}: {}", attempt + 1, imageUrl, e.getMessage());
                if (attempt == MAX_RETRIES - 1) throw new RuntimeException(e);
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

    private boolean fetchAndSavePanel(Chapter chapter, String dirName, String filename, String imageUrl, int pageNumber, int timeoutSeconds) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                byte[] bytes = imageWebClient.get()
                        .uri(imageUrl)
                        .retrieve()
                        .bodyToMono(byte[].class)
                        .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
                        .block();
                if (bytes != null && bytes.length > MAX_FILE_SIZE_BYTES) {
                    log.warn("Image too large ({} bytes), skipping: {}", bytes.length, imageUrl);
                    return false;
                }
                if (bytes != null && bytes.length > 0) {
                    java.nio.file.Path path = storageConfig.resolve(dirName, filename);
                    Files.write(path, bytes);
                    String format = filename.contains(".") ? filename.substring(filename.lastIndexOf('.') + 1) : "jpg";
                    Panel panel = Panel.builder()
                            .chapter(chapter)
                            .pageNumber(pageNumber)
                            .imageUrl(imageUrl)
                            .localPath(path.toString())
                            .fileSize((long) bytes.length)
                            .format(format)
                            .build();
                    chapter.getPanels().add(panel);
                    chapterRepository.saveAndFlush(chapter);
                    log.debug("Saved panel {}/{} for chapter {}", pageNumber, chapter.getTotalPanels(), chapter.getId());
                    return true;
                }
            } catch (WebClientResponseException e) {
                int status = e.getStatusCode().value();
                log.warn("Attempt {} failed for {}: HTTP {}", attempt + 1, imageUrl, status);
                if (attempt == MAX_RETRIES - 1) {
                    log.error("Giving up after {} attempts for {}", MAX_RETRIES, imageUrl);
                    throw new RuntimeException("Failed to download image: HTTP " + status, e);
                }
            } catch (Exception e) {
                log.warn("Attempt {} failed for {}: {}", attempt + 1, imageUrl, e.getMessage());
                if (attempt == MAX_RETRIES - 1) throw new RuntimeException(e);
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

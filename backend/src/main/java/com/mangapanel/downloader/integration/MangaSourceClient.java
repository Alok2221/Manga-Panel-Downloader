package com.mangapanel.downloader.integration;

import com.mangapanel.downloader.integration.mangadex.dto.*;
import reactor.core.publisher.Mono;

import java.util.List;

public interface MangaSourceClient {

    String extractChapterIdFromUrl(String chapterUrl);

    Mono<MangadexChapterResponse> fetchChapter(String chapterId);

    Mono<MangadexMangaResponse> fetchManga(String mangaId);

    Mono<MangadexGroupResponse> fetchGroup(String groupId);

    Mono<AtHomeResponse> fetchAtHomeServer(String chapterId);

    void reportAtHomeLoadResult(String imageUrl, boolean success, boolean cached, int bytes, int durationMs);

    String buildImageUrl(String baseUrl, String chapterHash, String filename);

    List<String> getFilenames(AtHomeResponse atHome);

    long getRateLimitDelayMs();

    int getHttpTimeoutSeconds();

    String getUploadBaseUrl();

    int getMaxFileSizeBytes();

    int getRetryAttempts();

    String buildChapterPageUrl(String chapterId);

    Mono<MangadexMangaListResponse> searchManga(String title, int limit, int offset);

    Mono<MangadexChapterFeedResponse> getMangaFeed(String mangaId, int limit, int offset, String translatedLanguage);

    Mono<MangadexCoverListResponse> fetchCoversForManga(String mangaId);
}


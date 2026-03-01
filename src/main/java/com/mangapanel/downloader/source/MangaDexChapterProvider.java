package com.mangapanel.downloader.source;

import com.mangapanel.downloader.client.MangaSourceApiResponse;
import com.mangapanel.downloader.client.MangaSourceClient;
import com.mangapanel.downloader.client.MangaSourceMangaResponse;
import com.mangapanel.downloader.dto.ChapterFetchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(1)
@Slf4j
@RequiredArgsConstructor
public class MangaDexChapterProvider implements ChapterSourceProvider {

    private static final String SOURCE_ID = "mangadex";

    private final MangaSourceClient mangaSourceClient;

    @Override
    public String getSourceId() {
        return SOURCE_ID;
    }

    @Override
    public boolean supports(String url) {
        return mangaSourceClient.extractChapterIdFromUrl(url) != null;
    }

    @Override
    public ChapterFetchResult fetchChapter(String url) {
        String chapterId = mangaSourceClient.extractChapterIdFromUrl(url);
        if (chapterId == null) {
            throw new IllegalArgumentException("Invalid MangaDex chapter URL");
        }
        MangaSourceApiResponse chapterResp = mangaSourceClient.fetchChapter(chapterId).block();
        if (chapterResp == null || chapterResp.getData() == null || chapterResp.getData().getAttributes() == null) {
            throw new IllegalStateException("Chapter not found or invalid response from MangaDex");
        }
        MangaSourceApiResponse.ChapterData data = chapterResp.getData();
        String mangaId = data.getRelationships() == null ? null : data.getRelationships().stream()
                .filter(r -> "manga".equals(r.getType()))
                .map(MangaSourceApiResponse.MangaSourceRelationship::getId)
                .findFirst()
                .orElse(null);

        String mangaTitle = "Unknown";
        if (mangaId != null) {
            try {
                MangaSourceMangaResponse mangaResp = mangaSourceClient.fetchManga(mangaId).block();
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
        return ChapterFetchResult.builder()
                .sourceId(SOURCE_ID)
                .mangaTitle(mangaTitle)
                .chapterTitle(chapterTitle != null ? chapterTitle : "Ch. " + data.getAttributes().getChapter())
                .chapterNumber(chapterNum)
                .language(data.getAttributes().getTranslatedLanguage())
                .imageUrls(null)
                .sourceChapterId(chapterId)
                .build();
    }
}

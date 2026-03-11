package com.mangapanel.downloader.service;

import com.mangapanel.downloader.integration.MangaSourceClient;
import com.mangapanel.downloader.integration.mangadex.dto.MangadexChapterFeedResponse;
import com.mangapanel.downloader.integration.mangadex.dto.MangadexMangaListResponse;
import com.mangapanel.downloader.web.dto.MangadexChapterDto;
import com.mangapanel.downloader.web.dto.MangadexMangaDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MangadexApiService {

    private final MangaSourceClient mangaSourceClient;

    /**
     * Search manga by title via MangaDex API.
     */
    public Mono<MangadexSearchResult> searchManga(String title, int limit, int offset) {
        if (title == null || title.isBlank()) {
            return Mono.just(new MangadexSearchResult(Collections.emptyList(), 0));
        }
        return mangaSourceClient.searchManga(title, limit, offset)
                .map(resp -> {
                    List<MangadexMangaDto> list = (resp.getData() == null)
                            ? List.of()
                            : resp.getData().stream()
                                    .map(e -> MangadexMangaDto.builder()
                                            .id(e.getId())
                                            .title(e.getDisplayTitle())
                                            .description(descriptionFrom(e.getAttributes()))
                                            .status(e.getAttributes() != null ? e.getAttributes().getStatus() : null)
                                            .year(e.getAttributes() != null ? e.getAttributes().getYear() : null)
                                            .contentRating(e.getAttributes() != null ? e.getAttributes().getContentRating() : null)
                                            .build())
                                    .collect(Collectors.toList());
                    int total = resp.getTotal() != null ? resp.getTotal() : list.size();
                    return new MangadexSearchResult(list, total);
                })
                .onErrorReturn(new MangadexSearchResult(Collections.emptyList(), 0));
    }

    /**
     * Get chapter feed for a manga via MangaDex API.
     */
    public Mono<MangadexFeedResult> getMangaChapters(String mangaId, int limit, int offset, String translatedLanguage) {
        if (mangaId == null || mangaId.isBlank()) {
            return Mono.just(new MangadexFeedResult(Collections.emptyList(), 0));
        }
        return mangaSourceClient.getMangaFeed(mangaId, limit, offset, translatedLanguage)
                .map(resp -> {
                    List<MangadexChapterDto> list = (resp.getData() == null)
                            ? List.of()
                            : resp.getData().stream()
                                    .map(e -> {
                                        MangadexChapterFeedResponse.MangadexChapterAttributes attrs = e.getAttributes();
                                        return MangadexChapterDto.builder()
                                                .id(e.getId())
                                                .title(attrs != null ? attrs.getTitle() : null)
                                                .chapter(attrs != null ? attrs.getChapter() : null)
                                                .volume(attrs != null ? attrs.getVolume() : null)
                                                .translatedLanguage(attrs != null ? attrs.getTranslatedLanguage() : null)
                                                .publishAt(parseInstant(attrs != null ? attrs.getPublishAt() : null))
                                                .chapterUrl(e.getChapterUrl())
                                                .build();
                                    })
                                    .collect(Collectors.toList());
                    int total = resp.getTotal() != null ? resp.getTotal() : list.size();
                    return new MangadexFeedResult(list, total);
                })
                .onErrorReturn(new MangadexFeedResult(Collections.emptyList(), 0));
    }

    private static String descriptionFrom(MangadexMangaListResponse.MangadexMangaAttributes attrs) {
        if (attrs == null || attrs.getDescription() == null) return null;
        var desc = attrs.getDescription();
        if (desc.containsKey("en")) return desc.get("en");
        return desc.values().stream().filter(v -> v != null && !v.isBlank()).findFirst().orElse(null);
    }

    private static Instant parseInstant(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Instant.parse(s);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public record MangadexSearchResult(List<MangadexMangaDto> data, int total) {}
    public record MangadexFeedResult(List<MangadexChapterDto> data, int total) {}
}

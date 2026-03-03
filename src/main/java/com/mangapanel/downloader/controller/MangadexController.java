package com.mangapanel.downloader.controller;

import com.mangapanel.downloader.dto.MangadexChapterDto;
import com.mangapanel.downloader.dto.MangadexMangaDto;
import com.mangapanel.downloader.service.MangadexApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * REST controller for MangaDex API proxy: search manga and list chapters.
 * Data is fetched from <a href="https://api.mangadex.org">api.mangadex.org</a>.
 */
@RestController
@RequestMapping("/api/mangadex")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:4200", "http://frontend:4200"}, allowCredentials = "true")
public class MangadexController {

    private final MangadexApiService mangadexApiService;

    /**
     * Search manga by title.
     * GET /api/mangadex/manga?title=...&limit=20&offset=0
     */
    @GetMapping(value = "/manga", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> searchManga(
            @RequestParam(required = false, defaultValue = "") String title,
            @RequestParam(required = false, defaultValue = "20") int limit,
            @RequestParam(required = false, defaultValue = "0") int offset) {
        return mangadexApiService.searchManga(title, limit, offset)
                .map(r -> Map.<String, Object>of(
                        "data", r.data(),
                        "total", r.total()
                ));
    }

    /**
     * Get chapters for a manga (MangaDex UUID).
     * GET /api/mangadex/manga/{id}/chapters?limit=96&offset=0&translatedLanguage=en
     */
    @GetMapping(value = "/manga/{id}/chapters", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> getMangaChapters(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "96") int limit,
            @RequestParam(required = false, defaultValue = "0") int offset,
            @RequestParam(required = false) String translatedLanguage) {
        return mangadexApiService.getMangaChapters(id, limit, offset, translatedLanguage)
                .map(r -> Map.<String, Object>of(
                        "data", r.data(),
                        "total", r.total()
                ));
    }
}

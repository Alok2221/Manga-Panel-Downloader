package com.mangapanel.downloader.web.controller;

import com.mangapanel.downloader.service.MangadexApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/mangadex")
@RequiredArgsConstructor
public class MangadexController {

    private final MangadexApiService mangadexApiService;

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

package com.mangapanel.downloader.controller;

import com.mangapanel.downloader.dto.ChapterDto;
import com.mangapanel.downloader.dto.ChapterGroupedDto;
import com.mangapanel.downloader.dto.DownloadRequest;
import com.mangapanel.downloader.dto.ErrorResponse;
import com.mangapanel.downloader.dto.PanelDto;
import com.mangapanel.downloader.service.ChapterDownloadService;
import com.mangapanel.downloader.service.ChapterService;
import com.mangapanel.downloader.service.PanelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:4200", "http://frontend:4200"}, allowCredentials = "true")
public class ChapterController {

    private final ChapterDownloadService downloadService;
    private final ChapterService chapterService;
    private final PanelService panelService;

    @GetMapping
    public ResponseEntity<java.util.Map<String, String>> apiInfo() {
        return ResponseEntity.ok(java.util.Map.of(
                "status", "ok",
                "message", "Manga Panel Downloader API"
        ));
    }

    @PostMapping("/download")
    public ResponseEntity<?> startDownload(@Valid @RequestBody DownloadRequest request) {
        try {
            var chapter = downloadService.createChapterAndStartDownload(request.getChapterUrl());
            downloadService.startPanelDownloadAsync(chapter.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(chapterService.toDto(chapter));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/chapters")
    public Page<ChapterDto> listChapters(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) BigDecimal chapter,
            @PageableDefault(size = 20) Pageable pageable) {
        return chapterService.search(title, chapter, pageable);
    }

    @GetMapping("/chapters/grouped")
    public List<ChapterGroupedDto> getChaptersGrouped(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) BigDecimal chapter,
            @RequestParam(required = false) String volume) {
        return chapterService.findGroupedByMangaAndVolume(title, chapter, volume);
    }

    /**
     * Chapter sequence for reader navigation when reading "all chapters" of a manga.
     * Uses exact manga title match (case-insensitive).
     */
    @GetMapping("/chapters/sequence")
    public List<ChapterDto> getChapterSequence(@RequestParam String mangaTitle) {
        return chapterService.getChapterSequenceForMangaTitle(mangaTitle);
    }

    @GetMapping("/chapters/{id}")
    public ResponseEntity<ChapterDto> getChapter(@PathVariable Long id) {
        return chapterService.findDtoById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/chapters/{id}/panels")
    public List<PanelDto> listPanels(@PathVariable Long id) {
        return panelService.findByChapterId(id);
    }

    @GetMapping(value = "/panels/{id}/image", produces = {MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, "image/webp", "image/gif"})
    public ResponseEntity<byte[]> getPanelImage(@PathVariable Long id) {
        return panelService.getImageBytes(id)
                .map(bytes -> {
                    String contentType = panelService.getContentType(id).orElse(MediaType.IMAGE_PNG_VALUE);
                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(contentType))
                            .body(bytes);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/chapters/{id}")
    public ResponseEntity<Void> deleteChapter(@PathVariable Long id) {
        if (chapterService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        chapterService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reindex chapter IDs to 1, 2, 3, ... (by current id order).
     * Call after deletions so the first remaining chapter has id=1.
     */
    @PostMapping("/chapters/reindex")
    public ResponseEntity<java.util.Map<String, String>> reindexChapters() {
        chapterService.reindexChapters();
        return ResponseEntity.ok(java.util.Map.of("status", "ok", "message", "Chapters reindexed"));
    }

    @GetMapping("/search")
    public Page<ChapterDto> search(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) BigDecimal chapter,
            @PageableDefault(size = 20) Pageable pageable) {
        return chapterService.search(title, chapter, pageable);
    }
}

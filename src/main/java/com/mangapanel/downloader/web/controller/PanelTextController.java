package com.mangapanel.downloader.web.controller;

import com.mangapanel.downloader.service.ChapterService;
import com.mangapanel.downloader.service.PanelTextService;
import com.mangapanel.downloader.web.dto.PanelTextSegmentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chapters")
@RequiredArgsConstructor
public class PanelTextController {

    private final PanelTextService panelTextService;
    private final ChapterService chapterService;

    @PostMapping("/{id}/ocr")
    public ResponseEntity<?> performOcr(
            @PathVariable Long id,
            @RequestParam(required = false) String sourceLanguage) {
        if (chapterService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        panelTextService.performOcr(id, sourceLanguage);
        return ResponseEntity.ok(Map.of("status", "completed"));
    }

    @GetMapping("/{id}/texts")
    public ResponseEntity<List<PanelTextSegmentDto>> getTexts(@PathVariable Long id) {
        if (chapterService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<PanelTextSegmentDto> segments = panelTextService.getByChapter(id);
        return ResponseEntity.ok(segments);
    }

    @PostMapping("/{id}/translate")
    public ResponseEntity<?> performTranslate(
            @PathVariable Long id,
            @RequestParam(required = false) String targetLanguage) {
        if (chapterService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        panelTextService.performTranslate(id, targetLanguage);
        return ResponseEntity.ok(Map.of("status", "completed"));
    }
}

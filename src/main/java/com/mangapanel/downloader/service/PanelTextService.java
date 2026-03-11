package com.mangapanel.downloader.service;

import com.mangapanel.downloader.entity.Panel;
import com.mangapanel.downloader.entity.PanelTextSegment;
import com.mangapanel.downloader.integration.translator.TranslatorClient;
import com.mangapanel.downloader.integration.translator.dto.*;
import com.mangapanel.downloader.repository.PanelRepository;
import com.mangapanel.downloader.repository.PanelTextSegmentRepository;
import com.mangapanel.downloader.web.dto.PanelTextSegmentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Service for OCR and translation of panel text segments.
 * Uses TranslatorClient to call the FastAPI translator service; persists/reads via
 * PanelTextSegmentRepository and PanelRepository.
 */
@Service
@RequiredArgsConstructor
public class PanelTextService {

    private static final String DEFAULT_SOURCE_LANG = "en";
    private static final String DEFAULT_TARGET_LANG = "pl";

    private final TranslatorClient translatorClient;
    private final PanelTextSegmentRepository panelTextSegmentRepository;
    private final PanelRepository panelRepository;
    private final PanelService panelService;
    private final ChapterService chapterService;

    /**
     * Get all text segments for a chapter, ordered by panel and sequence.
     */
    public List<PanelTextSegmentDto> getByChapter(Long chapterId) {
        return panelTextSegmentRepository.findByChapterIdOrderByPanelAndSequence(chapterId).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Perform OCR on all panels for the chapter, save segments to DB.
     */
    @Transactional
    public void performOcr(Long chapterId, String sourceLanguage) {
        List<Panel> panels = panelRepository.findByChapterIdOrderByPageNumberAsc(chapterId);
        if (panels.isEmpty()) {
            return;
        }

        List<PanelImageDto> panelImages = panels.stream()
                .map(p -> {
                    Optional<byte[]> data = panelService.getImageBytes(p.getId());
                    String base64 = data.map(bytes -> Base64.getEncoder().encodeToString(bytes)).orElse(null);
                    return PanelImageDto.builder()
                            .id(p.getId())
                            .imageBase64(base64)
                            .build();
                })
                .filter(p -> p.getImageBase64() != null && !p.getImageBase64().isBlank())
                .toList();

        if (panelImages.isEmpty()) {
            return;
        }

        OcrResponseDto response = translatorClient.performOcr(chapterId, panelImages,
                sourceLanguage != null ? sourceLanguage : DEFAULT_SOURCE_LANG);

        if (response == null || response.getSegments() == null || response.getSegments().isEmpty()) {
            return;
        }

        List<Long> panelIds = panels.stream().map(Panel::getId).toList();
        panelTextSegmentRepository.deleteByPanel_IdIn(panelIds);

        for (OcrSegmentDto seg : response.getSegments()) {
            Panel panel = panelRepository.getReferenceById(seg.getPanelId());
            PanelTextSegment entity = PanelTextSegment.builder()
                    .panel(panel)
                    .sequenceIndex(seg.getSequenceIndex())
                    .sourceLanguage(sourceLanguage != null ? sourceLanguage : DEFAULT_SOURCE_LANG)
                    .targetLanguage(DEFAULT_TARGET_LANG)
                    .sourceText(seg.getText())
                    .translatedText(null)
                    .bboxX(seg.getBboxX())
                    .bboxY(seg.getBboxY())
                    .bboxW(seg.getBboxW())
                    .bboxH(seg.getBboxH())
                    .build();
            panelTextSegmentRepository.save(entity);
        }
    }

    /**
     * Translate existing segments for the chapter (EN→PL by default).
     */
    @Transactional
    public void performTranslate(Long chapterId, String targetLanguage) {
        List<PanelTextSegment> segments = panelTextSegmentRepository.findByChapterIdOrderByPanelAndSequence(chapterId);
        if (segments.isEmpty()) {
            return;
        }

        String target = targetLanguage != null ? targetLanguage : DEFAULT_TARGET_LANG;
        TranslateContextDto context = buildContext(chapterId);

        List<TranslateSegmentInDto> inSegments = segments.stream()
                .map(s -> TranslateSegmentInDto.builder()
                        .id(s.getId())
                        .panelId(s.getPanel().getId())
                        .sourceText(s.getSourceText())
                        .build())
                .toList();

        TranslateRequestDto request = TranslateRequestDto.builder()
                .segments(inSegments)
                .sourceLanguage(DEFAULT_SOURCE_LANG)
                .targetLanguage(target)
                .context(context)
                .build();

        TranslateResponseDto response = translatorClient.translateSegments(request);
        if (response == null || response.getSegments() == null) {
            return;
        }

        var idToTranslation = response.getSegments().stream()
                .collect(java.util.stream.Collectors.toMap(TranslateSegmentOutDto::getId, TranslateSegmentOutDto::getTranslatedText));

        for (PanelTextSegment seg : segments) {
            String translated = idToTranslation.get(seg.getId());
            if (translated != null) {
                seg.setTranslatedText(translated);
                seg.setTargetLanguage(target);
                panelTextSegmentRepository.save(seg);
            }
        }
    }

    private TranslateContextDto buildContext(Long chapterId) {
        return chapterService.findById(chapterId)
                .map(ch -> TranslateContextDto.builder()
                        .mangaTitle(ch.getManga() != null ? ch.getManga().getTitle() : null)
                        .chapterNumber(ch.getChapterNumber() != null ? ch.getChapterNumber().toString() : null)
                        .build())
                .orElse(TranslateContextDto.builder().build());
    }

    private PanelTextSegmentDto toDto(PanelTextSegment s) {
        return PanelTextSegmentDto.builder()
                .id(s.getId())
                .panelId(s.getPanel().getId())
                .sequenceIndex(s.getSequenceIndex())
                .sourceLanguage(s.getSourceLanguage())
                .targetLanguage(s.getTargetLanguage())
                .sourceText(s.getSourceText())
                .translatedText(s.getTranslatedText())
                .bboxX(s.getBboxX())
                .bboxY(s.getBboxY())
                .bboxW(s.getBboxW())
                .bboxH(s.getBboxH())
                .build();
    }
}

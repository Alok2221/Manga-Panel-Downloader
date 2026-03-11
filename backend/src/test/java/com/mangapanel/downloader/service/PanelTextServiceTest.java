package com.mangapanel.downloader.service;

import com.mangapanel.downloader.entity.Chapter;
import com.mangapanel.downloader.entity.Panel;
import com.mangapanel.downloader.entity.PanelTextSegment;
import com.mangapanel.downloader.integration.translator.TranslatorClient;
import com.mangapanel.downloader.integration.translator.dto.*;
import com.mangapanel.downloader.repository.PanelRepository;
import com.mangapanel.downloader.repository.PanelTextSegmentRepository;
import com.mangapanel.downloader.web.dto.PanelTextSegmentDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PanelTextServiceTest {

    private TranslatorClient translatorClient;
    private PanelTextSegmentRepository panelTextSegmentRepository;
    private PanelRepository panelRepository;
    private PanelService panelService;
    private ChapterService chapterService;
    private PanelTextService panelTextService;

    @BeforeEach
    void setUp() {
        translatorClient = mock(TranslatorClient.class);
        panelTextSegmentRepository = mock(PanelTextSegmentRepository.class);
        panelRepository = mock(PanelRepository.class);
        panelService = mock(PanelService.class);
        chapterService = mock(ChapterService.class);
        panelTextService = new PanelTextService(
                translatorClient,
                panelTextSegmentRepository,
                panelRepository,
                panelService,
                chapterService
        );
    }

    @Test
    void getByChapter_returnsSegmentsMappedToDto() {
        Panel panel = Panel.builder().id(10L).build();
        PanelTextSegment seg = PanelTextSegment.builder()
                .id(1L)
                .panel(panel)
                .sequenceIndex(0)
                .sourceLanguage("en")
                .targetLanguage("pl")
                .sourceText("Hello")
                .translatedText("Cześć")
                .build();
        when(panelTextSegmentRepository.findByChapterIdOrderByPanelAndSequence(5L))
                .thenReturn(List.of(seg));

        List<PanelTextSegmentDto> result = panelTextService.getByChapter(5L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getPanelId()).isEqualTo(10L);
        assertThat(result.get(0).getSourceText()).isEqualTo("Hello");
        assertThat(result.get(0).getTranslatedText()).isEqualTo("Cześć");
        verify(panelTextSegmentRepository).findByChapterIdOrderByPanelAndSequence(5L);
    }

    @Test
    void getByChapter_emptyWhenNoSegments() {
        when(panelTextSegmentRepository.findByChapterIdOrderByPanelAndSequence(5L)).thenReturn(List.of());

        List<PanelTextSegmentDto> result = panelTextService.getByChapter(5L);

        assertThat(result).isEmpty();
    }

    @Test
    void performTranslate_updatesSegmentsFromTranslatorResponse() {
        Panel panel = Panel.builder().id(20L).build();
        PanelTextSegment seg1 = PanelTextSegment.builder()
                .id(100L)
                .panel(panel)
                .sequenceIndex(0)
                .sourceText("Hi")
                .translatedText(null)
                .build();
        when(panelTextSegmentRepository.findByChapterIdOrderByPanelAndSequence(7L)).thenReturn(List.of(seg1));
        when(chapterService.findById(7L)).thenReturn(Optional.of(Chapter.builder().build()));

        TranslateSegmentOutDto out = TranslateSegmentOutDto.builder().id(100L).translatedText("Cześć").build();
        when(translatorClient.translateSegments(any(TranslateRequestDto.class)))
                .thenReturn(TranslateResponseDto.builder().segments(List.of(out)).build());

        panelTextService.performTranslate(7L, "pl");

        verify(translatorClient).translateSegments(argThat(req ->
                req.getSegments().size() == 1
                        && req.getSegments().get(0).getSourceText().equals("Hi")
                        && "pl".equals(req.getTargetLanguage())));
        assertThat(seg1.getTranslatedText()).isEqualTo("Cześć");
        assertThat(seg1.getTargetLanguage()).isEqualTo("pl");
        verify(panelTextSegmentRepository).save(seg1);
    }

    @Test
    void performTranslate_doesNothingWhenNoSegments() {
        when(panelTextSegmentRepository.findByChapterIdOrderByPanelAndSequence(9L)).thenReturn(List.of());

        panelTextService.performTranslate(9L, "pl");

        verify(translatorClient, never()).translateSegments(any());
    }
}

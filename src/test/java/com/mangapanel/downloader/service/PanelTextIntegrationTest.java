package com.mangapanel.downloader.service;

import com.mangapanel.downloader.entity.Chapter;
import com.mangapanel.downloader.entity.Manga;
import com.mangapanel.downloader.entity.Panel;
import com.mangapanel.downloader.integration.translator.TranslatorClient;
import com.mangapanel.downloader.integration.translator.dto.OcrResponseDto;
import com.mangapanel.downloader.integration.translator.dto.OcrSegmentDto;
import com.mangapanel.downloader.repository.PanelTextSegmentRepository;
import com.mangapanel.downloader.repository.PanelRepository;
import com.mangapanel.downloader.repository.ChapterRepository;
import com.mangapanel.downloader.repository.MangaRepository;
import com.mangapanel.downloader.web.dto.PanelTextSegmentDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest
class PanelTextIntegrationTest {

    @Autowired
    private PanelTextService panelTextService;

    @Autowired
    private PanelTextSegmentRepository panelTextSegmentRepository;

    @Autowired
    private PanelRepository panelRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private MangaRepository mangaRepository;

    @MockitoBean
    private TranslatorClient translatorClient;

    private Long chapterId;
    private Long panelId;

    @BeforeEach
    void setUp() {
        panelTextSegmentRepository.deleteAll();
        panelRepository.deleteAll();
        chapterRepository.deleteAll();
        mangaRepository.deleteAll();

        Manga manga = mangaRepository.save(
                Manga.builder().title("Test Manga").createdAt(Instant.now()).build());
        Chapter ch = chapterRepository.save(
                Chapter.builder()
                        .manga(manga)
                        .chapterNumber(BigDecimal.ONE)
                        .title("Ch1")
                        .url("https://mangadex.org/chapter/test-1")
                        .totalPanels(1)
                        .downloadedAt(Instant.now())
                        .language("en")
                        .build());
        chapterId = ch.getId();
        Panel panel = panelRepository.save(
                Panel.builder()
                        .chapter(ch)
                        .pageNumber(1)
                        .data(new byte[]{1, 2, 3})
                        .format("png")
                        .build());
        panelId = panel.getId();
    }

    @Test
    void performOcr_savesSegmentsFromTranslatorResponse() {
        OcrSegmentDto segment = OcrSegmentDto.builder()
                .panelId(panelId)
                .sequenceIndex(0)
                .text("Hello")
                .build();
        when(translatorClient.performOcr(eq(chapterId), anyList(), anyString()))
                .thenReturn(OcrResponseDto.builder().segments(List.of(segment)).build());

        panelTextService.performOcr(chapterId, "en");

        List<PanelTextSegmentDto> saved = panelTextService.getByChapter(chapterId);
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getPanelId()).isEqualTo(panelId);
        assertThat(saved.get(0).getSourceText()).isEqualTo("Hello");
        assertThat(saved.get(0).getTranslatedText()).isNull();
    }
}

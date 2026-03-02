package com.mangapanel.downloader.controller;

import com.mangapanel.downloader.entity.Chapter;
import com.mangapanel.downloader.entity.Manga;
import com.mangapanel.downloader.entity.Panel;
import com.mangapanel.downloader.repository.ChapterRepository;
import com.mangapanel.downloader.repository.MangaRepository;
import com.mangapanel.downloader.repository.PanelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ChapterControllerIntegrationTest {

    @Autowired
    private MangaRepository mangaRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private PanelRepository panelRepository;

    private Long chapterId;
    private Long panelId;

    @BeforeEach
    void setUp() {
        panelRepository.deleteAll();
        chapterRepository.deleteAll();
        mangaRepository.deleteAll();

        Manga manga = mangaRepository.save(
                Manga.builder()
                        .title("Bleach")
                        .createdAt(Instant.now())
                        .build()
        );

        Chapter ch = chapterRepository.save(
                Chapter.builder()
                        .manga(manga)
                        .chapterNumber(BigDecimal.valueOf(336))
                        .title("El Verdugo")
                        .url("https://mangadex.org/chapter/test-chapter-id")
                        .totalPanels(1)
                        .downloadedAt(Instant.now())
                        .language("en")
                        .build()
        );
        chapterId = ch.getId();

        Panel panel = panelRepository.save(
                Panel.builder()
                        .chapter(ch)
                        .pageNumber(1)
                        .imageUrl("https://example.org/image.jpg")
                        .data(new byte[]{1, 2, 3})
                        .fileSize(3L)
                        .format("jpg")
                        .build()
        );
        panelId = panel.getId();
    }

    @Test
    void basicPersistence_setupWorks() {
        assertThat(mangaRepository.count()).isEqualTo(1);
        assertThat(chapterRepository.count()).isEqualTo(1);
        assertThat(panelRepository.count()).isEqualTo(1);
    }
}


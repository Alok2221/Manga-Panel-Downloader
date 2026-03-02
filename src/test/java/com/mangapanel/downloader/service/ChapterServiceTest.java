package com.mangapanel.downloader.service;

import com.mangapanel.downloader.dto.ChapterDto;
import com.mangapanel.downloader.entity.Chapter;
import com.mangapanel.downloader.entity.Manga;
import com.mangapanel.downloader.repository.ChapterRepository;
import com.mangapanel.downloader.repository.PanelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ChapterServiceTest {

    private ChapterRepository chapterRepository;
    private PanelRepository panelRepository;
    private ChapterService chapterService;

    @BeforeEach
    void setUp() {
        chapterRepository = mock(ChapterRepository.class);
        panelRepository = mock(PanelRepository.class);
        chapterService = new ChapterService(chapterRepository, panelRepository);
    }

    @Test
    void toDto_populatesPanelsDownloadedFromRepositoryCount() {
        Manga manga = Manga.builder()
                .id(1L)
                .title("Bleach")
                .createdAt(Instant.now())
                .build();
        Chapter chapter = Chapter.builder()
                .id(10L)
                .manga(manga)
                .chapterNumber(java.math.BigDecimal.valueOf(336))
                .title("El Verdugo")
                .url("https://mangadex.org/chapter/2af914dc-0d63-4ac0-875e-75390ce59390")
                .totalPanels(22)
                .downloadedAt(Instant.now())
                .language("en")
                .build();

        when(panelRepository.countByChapterId(10L)).thenReturn(22L);

        ChapterDto dto = chapterService.toDto(chapter);

        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getMangaId()).isEqualTo(1L);
        assertThat(dto.getMangaTitle()).isEqualTo("Bleach");
        assertThat(dto.getChapterNumber()).isEqualTo(java.math.BigDecimal.valueOf(336));
        assertThat(dto.getTitle()).isEqualTo("El Verdugo");
        assertThat(dto.getTotalPanels()).isEqualTo(22);
        assertThat(dto.getPanelsDownloaded()).isEqualTo(22);
        verify(panelRepository).countByChapterId(10L);
    }

    @Test
    void findDtoById_returnsEmptyWhenChapterMissing() {
        when(chapterRepository.findById(ArgumentMatchers.anyLong())).thenReturn(Optional.empty());

        Optional<ChapterDto> result = chapterService.findDtoById(123L);

        assertThat(result).isEmpty();
    }
}


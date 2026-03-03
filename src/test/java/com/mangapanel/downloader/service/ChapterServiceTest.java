package com.mangapanel.downloader.service;

import com.mangapanel.downloader.dto.ChapterDto;
import com.mangapanel.downloader.dto.ChapterGroupedDto;
import com.mangapanel.downloader.entity.Chapter;
import com.mangapanel.downloader.entity.Manga;
import com.mangapanel.downloader.repository.ChapterRepository;
import com.mangapanel.downloader.repository.PanelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ChapterServiceTest {

    private ChapterRepository chapterRepository;
    private PanelRepository panelRepository;
    private JdbcTemplate jdbcTemplate;
    private ChapterService chapterService;

    @BeforeEach
    void setUp() {
        chapterRepository = mock(ChapterRepository.class);
        panelRepository = mock(PanelRepository.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        chapterService = new ChapterService(chapterRepository, panelRepository, jdbcTemplate);
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

    @Test
    void findGroupedByMangaAndVolume_groupsChaptersByMangaAndVolume() {
        Manga manga = Manga.builder().id(1L).title("Bleach").createdAt(Instant.now()).build();
        Chapter ch1 = Chapter.builder()
                .id(1L).manga(manga).chapterNumber(BigDecimal.ONE).title("Ch.1").url("https://mangadex.org/chapter/u1")
                .totalPanels(10).downloadedAt(Instant.now()).language("en").volume("1")
                .build();
        Chapter ch2 = Chapter.builder()
                .id(2L).manga(manga).chapterNumber(BigDecimal.valueOf(2)).title("Ch.2").url("https://mangadex.org/chapter/u2")
                .totalPanels(12).downloadedAt(Instant.now()).language("en").volume("1")
                .build();
        Chapter ch3 = Chapter.builder()
                .id(3L).manga(manga).chapterNumber(BigDecimal.valueOf(3)).title("Ch.3").url("https://mangadex.org/chapter/u3")
                .totalPanels(8).downloadedAt(Instant.now()).language("en").volume(null)
                .build();
        when(panelRepository.countByChapterId(1L)).thenReturn(10L);
        when(panelRepository.countByChapterId(2L)).thenReturn(12L);
        when(panelRepository.countByChapterId(3L)).thenReturn(8L);
        when(chapterRepository.findForGrouped(null, null, null))
                .thenReturn(List.of(ch1, ch2, ch3));

        List<ChapterGroupedDto> result = chapterService.findGroupedByMangaAndVolume(null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMangaTitle()).isEqualTo("Bleach");
        assertThat(result.get(0).getMangaId()).isEqualTo(1L);
        assertThat(result.get(0).getVolumes()).hasSize(2);
        assertThat(result.get(0).getVolumes().get(0).getVolume()).isEqualTo("1");
        assertThat(result.get(0).getVolumes().get(0).getChapters()).hasSize(2);
        assertThat(result.get(0).getVolumes().get(1).getVolume()).isEqualTo("none");
        assertThat(result.get(0).getVolumes().get(1).getChapters()).hasSize(1);
    }
}


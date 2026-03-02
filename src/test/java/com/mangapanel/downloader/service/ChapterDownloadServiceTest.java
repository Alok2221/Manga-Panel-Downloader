package com.mangapanel.downloader.service;

import com.mangapanel.downloader.client.MangaSourceApiResponse;
import com.mangapanel.downloader.client.MangaSourceClient;
import com.mangapanel.downloader.client.MangaSourceMangaResponse;
import com.mangapanel.downloader.entity.Chapter;
import com.mangapanel.downloader.entity.Manga;
import com.mangapanel.downloader.repository.ChapterRepository;
import com.mangapanel.downloader.repository.MangaRepository;
import com.mangapanel.downloader.repository.PanelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ChapterDownloadServiceTest {

    private MangaSourceClient mangaSourceClient;
    private MangaRepository mangaRepository;
    private ChapterRepository chapterRepository;
    private PanelRepository panelRepository;
    private WebClient webClient;

    private ChapterDownloadService service;

    @BeforeEach
    void setUp() {
        mangaSourceClient = mock(MangaSourceClient.class);
        mangaRepository = mock(MangaRepository.class);
        chapterRepository = mock(ChapterRepository.class);
        panelRepository = mock(PanelRepository.class);
        webClient = mock(WebClient.class);
        service = new ChapterDownloadService(
                mangaSourceClient, mangaRepository, chapterRepository, panelRepository, webClient
        );
    }

    @Test
    void createChapterAndStartDownload_rejectsNonMangaDexUrl() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createChapterAndStartDownload("https://example.com/chapter/123"));
    }

    @Test
    void createChapterAndStartDownload_persistsMangaAndChapter() {
        String url = "https://mangadex.org/chapter/2af914dc-0d63-4ac0-875e-75390ce59390";

        when(chapterRepository.existsByUrl(url)).thenReturn(false);
        when(mangaSourceClient.extractChapterIdFromUrl(url)).thenReturn("2af914dc-0d63-4ac0-875e-75390ce59390");

        MangaSourceApiResponse.ChapterAttributes chapterAttributes = new MangaSourceApiResponse.ChapterAttributes();
        chapterAttributes.setChapter("336");
        chapterAttributes.setTitle("El Verdugo");
        chapterAttributes.setTranslatedLanguage("en");

        MangaSourceApiResponse.ChapterData data = new MangaSourceApiResponse.ChapterData();
        data.setId("2af914dc-0d63-4ac0-875e-75390ce59390");
        data.setAttributes(chapterAttributes);
        MangaSourceApiResponse apiResp = new MangaSourceApiResponse();
        apiResp.setData(data);

        when(mangaSourceClient.fetchChapter("2af914dc-0d63-4ac0-875e-75390ce59390")).thenReturn(Mono.just(apiResp));

        // Do not mock fetchManga here on purpose; ChapterDownloadService swallows failures and falls
        // back to "Unknown" title, which is acceptable for this unit test.

        when(mangaRepository.save(any(Manga.class))).thenAnswer(invocation -> {
            Manga m = invocation.getArgument(0);
            m.setId(1L);
            return m;
        });
        when(chapterRepository.save(any(Chapter.class))).thenAnswer(invocation -> {
            Chapter c = invocation.getArgument(0);
            c.setId(3L);
            return c;
        });

        Chapter result = service.createChapterAndStartDownload(url);

        assertThat(result.getId()).isEqualTo(3L);
        assertThat(result.getManga().getId()).isEqualTo(1L);
        assertThat(result.getManga().getTitle()).isNotBlank();
        assertThat(result.getChapterNumber()).isEqualTo(new java.math.BigDecimal("336"));
        assertThat(result.getTitle()).isEqualTo("El Verdugo");
        assertThat(result.getUrl()).isEqualTo(url);
        assertThat(result.getLanguage()).isEqualTo("en");
        assertThat(result.getDownloadedAt()).isBeforeOrEqualTo(Instant.now());
    }
}


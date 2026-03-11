package com.mangapanel.downloader.service;

import com.mangapanel.downloader.repository.PanelRepository;
import com.mangapanel.downloader.web.dto.PanelDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PanelServiceTest {

    private PanelRepository panelRepository;
    private PanelService panelService;

    @BeforeEach
    void setUp() {
        panelRepository = mock(PanelRepository.class);
        panelService = new PanelService(panelRepository);
    }

    @Test
    void findByChapterId_returnsDtosFromRepository() {
        PanelDto dto = PanelDto.builder()
                .id(1L)
                .chapterId(3L)
                .pageNumber(1)
                .imageUrl("http://example/image1.jpg")
                .fileSize(1234L)
                .format("jpg")
                .build();
        when(panelRepository.findDtosByChapterId(3L)).thenReturn(List.of(dto));

        List<PanelDto> result = panelService.findByChapterId(3L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        verify(panelRepository).findDtosByChapterId(3L);
    }

    @Test
    void getImageBytes_delegatesToRepositoryProjection() {
        byte[] bytes = new byte[]{1, 2, 3};
        when(panelRepository.findDataById(5L)).thenReturn(Optional.of(bytes));

        Optional<byte[]> result = panelService.getImageBytes(5L);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow()).containsExactly(1, 2, 3);
        verify(panelRepository).findDataById(5L);
    }

    @Test
    void getContentType_mapsFormatToMimeType() {
        when(panelRepository.findFormatById(1L)).thenReturn(Optional.of("jpg"));
        when(panelRepository.findFormatById(2L)).thenReturn(Optional.of("png"));
        when(panelRepository.findFormatById(3L)).thenReturn(Optional.of("webp"));
        when(panelRepository.findFormatById(4L)).thenReturn(Optional.of("gif"));

        assertThat(panelService.getContentType(1L)).hasValue("image/jpeg");
        assertThat(panelService.getContentType(2L)).hasValue("image/png");
        assertThat(panelService.getContentType(3L)).hasValue("image/webp");
        assertThat(panelService.getContentType(4L)).hasValue("image/gif");
    }
}


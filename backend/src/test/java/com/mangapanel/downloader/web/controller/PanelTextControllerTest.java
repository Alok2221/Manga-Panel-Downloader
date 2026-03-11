package com.mangapanel.downloader.web.controller;

import com.mangapanel.downloader.entity.Chapter;
import com.mangapanel.downloader.service.ChapterService;
import com.mangapanel.downloader.service.PanelTextService;
import com.mangapanel.downloader.web.dto.PanelTextSegmentDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for PanelTextController using standalone MockMvc (no WebMvcTest slice).
 */
class PanelTextControllerTest {

    private MockMvc mockMvc;
    private PanelTextService panelTextService;
    private ChapterService chapterService;

    @BeforeEach
    void setUp() {
        panelTextService = mock(PanelTextService.class);
        chapterService = mock(ChapterService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new PanelTextController(panelTextService, chapterService)).build();
    }

    @Test
    void getTexts_returns200AndSegmentsWhenChapterExists() throws Exception {
        when(chapterService.findById(1L)).thenReturn(Optional.of(Chapter.builder().id(1L).build()));
        PanelTextSegmentDto dto = PanelTextSegmentDto.builder()
                .id(1L)
                .panelId(10L)
                .sourceText("Hello")
                .build();
        when(panelTextService.getByChapter(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/chapters/1/texts"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].panelId").value(10))
                .andExpect(jsonPath("$[0].sourceText").value("Hello"));

        verify(chapterService).findById(1L);
        verify(panelTextService).getByChapter(1L);
    }

    @Test
    void getTexts_returns404WhenChapterNotFound() throws Exception {
        when(chapterService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/chapters/999/texts"))
                .andExpect(status().isNotFound());

        verify(chapterService).findById(999L);
        verify(panelTextService, never()).getByChapter(anyLong());
    }

    @Test
    void performOcr_returns200WhenChapterExists() throws Exception {
        when(chapterService.findById(1L)).thenReturn(Optional.of(Chapter.builder().id(1L).build()));

        mockMvc.perform(post("/api/chapters/1/ocr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"));

        verify(chapterService).findById(1L);
        verify(panelTextService).performOcr(eq(1L), isNull());
    }

    @Test
    void performOcr_returns404WhenChapterNotFound() throws Exception {
        when(chapterService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/chapters/999/ocr"))
                .andExpect(status().isNotFound());

        verify(panelTextService, never()).performOcr(anyLong(), any());
    }

    @Test
    void performTranslate_returns200WhenChapterExists() throws Exception {
        when(chapterService.findById(1L)).thenReturn(Optional.of(Chapter.builder().id(1L).build()));

        mockMvc.perform(post("/api/chapters/1/translate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"));

        verify(chapterService).findById(1L);
        verify(panelTextService).performTranslate(eq(1L), isNull());
    }

    @Test
    void performTranslate_returns404WhenChapterNotFound() throws Exception {
        when(chapterService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/chapters/999/translate"))
                .andExpect(status().isNotFound());

        verify(panelTextService, never()).performTranslate(anyLong(), any());
    }
}

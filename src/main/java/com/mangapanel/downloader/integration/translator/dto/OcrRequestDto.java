package com.mangapanel.downloader.integration.translator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrRequestDto {
    @JsonProperty("chapter_id")
    private Long chapterId;
    private List<PanelImageDto> panels;
    @JsonProperty("source_language")
    private String sourceLanguage;
}

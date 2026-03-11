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
public class TranslateRequestDto {
    private List<TranslateSegmentInDto> segments;
    @JsonProperty("source_language")
    private String sourceLanguage;
    @JsonProperty("target_language")
    private String targetLanguage;
    private TranslateContextDto context;
}

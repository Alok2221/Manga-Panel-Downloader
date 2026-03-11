package com.mangapanel.downloader.integration.translator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslateSegmentInDto {
    private Long id;
    @JsonProperty("panel_id")
    private Long panelId;
    @JsonProperty("source_text")
    private String sourceText;
}

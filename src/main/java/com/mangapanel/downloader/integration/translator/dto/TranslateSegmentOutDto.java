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
public class TranslateSegmentOutDto {
    private Long id;
    @JsonProperty("translated_text")
    private String translatedText;
}

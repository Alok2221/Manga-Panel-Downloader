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
public class TranslateContextDto {
    @JsonProperty("manga_title")
    private String mangaTitle;
    @JsonProperty("chapter_number")
    private String chapterNumber;
    private String notes;
}

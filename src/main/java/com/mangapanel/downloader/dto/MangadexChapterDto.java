package com.mangapanel.downloader.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MangadexChapterDto {
    private String id;
    private String title;
    private String chapter;
    private String volume;
    private String translatedLanguage;
    private Instant publishAt;
    /** URL to use for POST /api/download (e.g. https://mangadex.org/chapter/{id}) */
    private String chapterUrl;
}

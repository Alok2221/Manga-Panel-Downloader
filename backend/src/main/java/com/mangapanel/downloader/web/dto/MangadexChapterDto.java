package com.mangapanel.downloader.web.dto;

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
    private String chapterUrl;
}

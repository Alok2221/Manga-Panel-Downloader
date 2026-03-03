package com.mangapanel.downloader.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChapterDto {
    private Long id;
    private Long mangaId;
    private String mangaTitle;
    private BigDecimal chapterNumber;
    private String title;
    private String url;
    private Integer totalPanels;
    private Integer panelsDownloaded;
    private Instant downloadedAt;
    private String language;
    private String volume;
}

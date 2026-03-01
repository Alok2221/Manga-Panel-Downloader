package com.mangapanel.downloader.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChapterFetchResult {
    private String sourceId;
    private String mangaTitle;
    private String chapterTitle;
    private java.math.BigDecimal chapterNumber;
    private String language;
    private List<String> imageUrls;
    private String sourceChapterId;
}

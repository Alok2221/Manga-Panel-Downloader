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
public class MangaDto {
    private Long id;
    private String title;
    private String originalTitle;
    private String author;
    private String description;
    private String coverUrl;
    private Instant createdAt;
}

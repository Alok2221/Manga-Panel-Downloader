package com.mangapanel.downloader.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MangadexMangaDto {
    private String id;
    private String title;
    private String description;
    private String status;
    private String year;
    private String contentRating;
}

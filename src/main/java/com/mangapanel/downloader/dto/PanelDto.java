package com.mangapanel.downloader.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PanelDto {
    private Long id;
    private Long chapterId;
    private Integer pageNumber;
    private String imageUrl;
    private String localPath;
    private Integer width;
    private Integer height;
    private Long fileSize;
    private String format;
}

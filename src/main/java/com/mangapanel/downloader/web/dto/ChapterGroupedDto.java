package com.mangapanel.downloader.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChapterGroupedDto {

    private Long mangaId;
    private String mangaTitle;
    private String mangaCoverUrl;
    private List<VolumeGroupDto> volumes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VolumeGroupDto {
        private String volume;
        private List<ChapterDto> chapters;
    }
}

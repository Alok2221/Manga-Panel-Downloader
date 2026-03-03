package com.mangapanel.downloader.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Response for GET /api/chapters/grouped: manga → volumes → chapters. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChapterGroupedDto {

    private Long mangaId;
    private String mangaTitle;
    private List<VolumeGroupDto> volumes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VolumeGroupDto {
        /** Volume identifier (e.g. "1", "2", "none"). */
        private String volume;
        private List<ChapterDto> chapters;
    }
}

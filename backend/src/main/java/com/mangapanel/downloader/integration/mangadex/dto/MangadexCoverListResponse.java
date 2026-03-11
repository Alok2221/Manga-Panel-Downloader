package com.mangapanel.downloader.integration.mangadex.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MangadexCoverListResponse {

    private String result;
    private List<CoverData> data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CoverData {
        private String id;
        private CoverAttributes attributes;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CoverAttributes {
        private String fileName;
    }
}


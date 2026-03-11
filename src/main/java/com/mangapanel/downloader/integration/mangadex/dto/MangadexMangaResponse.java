package com.mangapanel.downloader.integration.mangadex.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MangadexMangaResponse {

    private String result;
    private MangaData data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MangaData {
        private String id;
        private MangaAttributes attributes;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MangaAttributes {
        private Map<String, String> title;
        private Map<String, String> description;
        private Map<String, String> links;
    }
}


package com.mangapanel.downloader.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MangaSourceApiResponse {

    private String result;
    private ChapterData data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChapterData {
        private String id;
        private ChapterAttributes attributes;
        private List<MangaSourceRelationship> relationships;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChapterAttributes {
        private String title;
        private String chapter;
        private String volume;
        private String translatedLanguage;
        private String publishAt;
        private Integer pages;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MangaSourceRelationship {
        private String id;
        private String type;
    }
}

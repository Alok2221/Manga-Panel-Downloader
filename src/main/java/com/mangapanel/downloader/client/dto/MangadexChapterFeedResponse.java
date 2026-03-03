package com.mangapanel.downloader.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Response from MangaDex GET /manga/{id}/feed (chapters of a manga).
 * @see <a href="https://api.mangadex.org/docs/04-chapter/feed/">MangaDex API - Find a Manga's Chapters</a>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MangadexChapterFeedResponse {

    private String result;
    private String response;
    private List<MangadexChapterFeedEntry> data;
    private Integer limit;
    private Integer offset;
    private Integer total;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MangadexChapterFeedEntry {
        private String id;
        private String type;
        private MangadexChapterAttributes attributes;
        private List<MangadexRelationship> relationships;

        public String getChapterUrl() {
            return "https://mangadex.org/chapter/" + id;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MangadexChapterAttributes {
        private String title;
        private String chapter;
        private String volume;
        private String translatedLanguage;
        private String publishAt;
        private Integer pages;
        private String externalUrl;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MangadexRelationship {
        private String id;
        private String type;
    }
}

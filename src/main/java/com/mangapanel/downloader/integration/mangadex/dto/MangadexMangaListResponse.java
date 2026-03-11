package com.mangapanel.downloader.integration.mangadex.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MangadexMangaListResponse {

    private String result;
    private String response;
    private List<MangadexMangaEntry> data;
    private Integer limit;
    private Integer offset;
    private Integer total;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MangadexMangaEntry {
        private String id;
        private String type;
        private MangadexMangaAttributes attributes;
        private List<MangadexRelationship> relationships;

        public String getDisplayTitle() {
            if (attributes == null || attributes.getTitle() == null) return id;
            var title = attributes.getTitle();
            if (title.containsKey("en") && title.get("en") != null) return title.get("en");
            return title.values().stream().filter(t -> t != null && !t.isBlank()).findFirst().orElse(id);
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MangadexMangaAttributes {
        private java.util.Map<String, String> title;
        private java.util.Map<String, String> description;
        private String status;
        private String year;
        private String contentRating;
        private String publicationDemographic;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MangadexRelationship {
        private String id;
        private String type;
    }
}


package com.mangapanel.downloader.integration.mangadex.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MangadexGroupResponse {

    private String result;
    private GroupData data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GroupData {
        private String id;
        private GroupAttributes attributes;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GroupAttributes {
        private String name;
    }
}


package com.mangapanel.downloader.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AtHomeResponse {

    private String result;
    private String baseUrl;
    private ChapterInfo chapter;

    public void setResult(String result) {
        this.result = result;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setChapter(ChapterInfo chapter) {
        this.chapter = chapter;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChapterInfo {
        private String hash;
        @JsonProperty("data")
        private List<String> dataFilenames;
        @JsonProperty("dataSaver")
        private List<String> dataSaverFilenames;

        public void setHash(String hash) {
            this.hash = hash;
        }

        public void setDataFilenames(List<String> dataFilenames) {
            this.dataFilenames = dataFilenames;
        }

        public void setDataSaverFilenames(List<String> dataSaverFilenames) {
            this.dataSaverFilenames = dataSaverFilenames;
        }
    }
}

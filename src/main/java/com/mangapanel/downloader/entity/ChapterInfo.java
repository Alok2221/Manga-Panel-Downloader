package com.mangapanel.downloader.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChapterInfo {
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
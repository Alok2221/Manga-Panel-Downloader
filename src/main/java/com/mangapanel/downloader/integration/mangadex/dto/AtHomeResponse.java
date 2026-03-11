package com.mangapanel.downloader.integration.mangadex.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mangapanel.downloader.entity.ChapterInfo;
import lombok.Getter;

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
}


package com.mangapanel.downloader.source;

import com.mangapanel.downloader.dto.ChapterFetchResult;

public interface ChapterSourceProvider {

    String getSourceId();

    boolean supports(String url);

    ChapterFetchResult fetchChapter(String url);
}

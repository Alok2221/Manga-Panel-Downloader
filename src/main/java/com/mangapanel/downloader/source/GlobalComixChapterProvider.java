package com.mangapanel.downloader.source;

import com.mangapanel.downloader.client.GlobalComixClient;
import com.mangapanel.downloader.dto.ChapterFetchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(2)
@Slf4j
@RequiredArgsConstructor
public class GlobalComixChapterProvider implements ChapterSourceProvider {

    private static final String SOURCE_ID = "globalcomix";

    private final GlobalComixClient globalComixClient;

    @Override
    public String getSourceId() {
        return SOURCE_ID;
    }

    @Override
    public boolean supports(String url) {
        return globalComixClient.isGlobalComixUrl(url);
    }

    @Override
    public ChapterFetchResult fetchChapter(String url) {
        String html = globalComixClient.fetchPageHtml(url);
        List<String> imageUrls = globalComixClient.parseImageUrls(html, url);
        if (imageUrls.isEmpty()) {
            throw new IllegalStateException("No panel images found on this page. The reader might load content dynamically.");
        }
        String pageTitle = globalComixClient.parseTitle(html);
        return ChapterFetchResult.builder()
                .sourceId(SOURCE_ID)
                .mangaTitle(pageTitle != null && !pageTitle.isBlank() ? pageTitle : "GlobalComix")
                .chapterTitle(null)
                .chapterNumber(null)
                .language(null)
                .imageUrls(imageUrls)
                .sourceChapterId(null)
                .build();
    }
}

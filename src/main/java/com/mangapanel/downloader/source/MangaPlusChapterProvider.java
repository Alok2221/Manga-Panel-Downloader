package com.mangapanel.downloader.source;

import com.mangapanel.downloader.dto.ChapterFetchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
@Order(3)
@Slf4j
@RequiredArgsConstructor
public class MangaPlusChapterProvider implements ChapterSourceProvider {

    private static final String SOURCE_ID = "mangaplus";
    private static final Pattern MANGA_PLUS_VIEWER = Pattern.compile(
            "https?://(www\\.)?mangaplus\\.shueisha\\.co\\.jp/viewer/(\\d+)",
            Pattern.CASE_INSENSITIVE
    );
    private static final String API_BASE = "https://jumpg-webapi.tokyo-cdn.com/api";

    private final WebClient.Builder webClientBuilder;

    @Override
    public String getSourceId() {
        return SOURCE_ID;
    }

    @Override
    public boolean supports(String url) {
        if (url == null) return false;
        return MANGA_PLUS_VIEWER.matcher(url.trim()).find() || url.contains("mangaplus.shueisha");
    }

    @Override
    public ChapterFetchResult fetchChapter(String url) {
        var matcher = MANGA_PLUS_VIEWER.matcher(url.trim());
        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid MangaPlus viewer URL (expected .../viewer/CHAPTER_ID)");
        }
        String chapterId = matcher.group(2);
        String apiUrl = API_BASE + "/manga_viewer?chapter_id=" + chapterId + "&split=no";
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> json = webClientBuilder.build()
                    .get()
                    .uri(apiUrl)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(java.time.Duration.ofSeconds(15))
                    .block();
            if (json == null) throw new IllegalStateException("Empty response from MangaPlus");
            Map<String, Object> success = (Map<String, Object>) json.get("success");
            if (success == null) {
                Object error = json.get("error");
                throw new IllegalStateException("MangaPlus API error: " + (error != null ? error : json));
            }
            String mangaTitle = (String) success.get("titleName");
            String chapterTitle = (String) success.get("chapterName");
            List<Map<String, Object>> pages = (List<Map<String, Object>>) success.get("pages");
            if (pages == null || pages.isEmpty()) {
                throw new IllegalStateException("No pages in MangaPlus chapter");
            }
            List<String> imageUrls = new java.util.ArrayList<>();
            for (Map<String, Object> page : pages) {
                Map<String, Object> mangaPage = (Map<String, Object>) page.get("mangaPage");
                if (mangaPage != null) {
                    String imageUrl = (String) mangaPage.get("imageUrl");
                    if (imageUrl != null && !imageUrl.isBlank()) {
                        imageUrls.add(imageUrl);
                    }
                }
            }
            if (imageUrls.isEmpty()) {
                throw new IllegalStateException("No image URLs in MangaPlus chapter response");
            }
            return ChapterFetchResult.builder()
                    .sourceId(SOURCE_ID)
                    .mangaTitle(mangaTitle != null ? mangaTitle : "MangaPlus")
                    .chapterTitle(chapterTitle)
                    .chapterNumber(null)
                    .language("en")
                    .imageUrls(imageUrls)
                    .sourceChapterId(null)
                    .build();
        } catch (Exception e) {
            log.warn("MangaPlus API failed for chapter {}: {}", chapterId, e.getMessage());
            throw new IllegalStateException("Could not fetch MangaPlus chapter: " + e.getMessage(), e);
        }
    }
}

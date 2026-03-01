package com.mangapanel.downloader.client;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GlobalComixClient {

    private static final Logger log = LoggerFactory.getLogger(GlobalComixClient.class);
    private static final Pattern GLOBALCOMIX_URL = Pattern.compile(
            "https?://(www\\.)?globalcomix\\.com/",
            Pattern.CASE_INSENSITIVE
    );
    private static final int HTTP_TIMEOUT_SECONDS = 15;
    private static final long RATE_LIMIT_MS = 300;

    private final WebClient webClient;

    public GlobalComixClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .defaultHeader(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build();
    }

    public boolean isGlobalComixUrl(String url) {
        return url != null && GLOBALCOMIX_URL.matcher(url.trim()).find();
    }

    public String fetchPageHtml(String pageUrl) {
        return webClient.get()
                .uri(pageUrl)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(java.time.Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
                .block();
    }

    private static final Pattern CDN_URL_IN_HTML = Pattern.compile(
            "https://[^\"'\\s<>]*digitaloceanspaces\\.com[^\"'\\s<>]*\\.(?:jpg|jpeg|png|webp)[^\"'\\s<>]*",
            Pattern.CASE_INSENSITIVE
    );

    public List<String> parseImageUrls(String html, String baseUrl) {
        Set<String> seen = new LinkedHashSet<>();
        if (html == null || html.isBlank()) return new ArrayList<>();

        Document doc = baseUrl != null ? Jsoup.parse(html, baseUrl) : Jsoup.parse(html);
        Elements imgs = doc.select("img[src*=digitaloceanspaces.com], img[src*=globalcomix-comics-assets]");
        for (Element img : imgs) {
            String src = img.attr("abs:src");
            if (src == null || src.isBlank()) src = img.attr("src");
            if (src != null && !src.isBlank()) {
                src = src.replace("&amp;", "&");
                if (src.startsWith("http") && (src.contains("digitaloceanspaces.com") || src.contains("globalcomix-comics-assets"))) {
                    seen.add(src);
                }
            }
        }
        if (seen.isEmpty()) {
            Matcher m = CDN_URL_IN_HTML.matcher(html);
            while (m.find()) {
                String u = m.group().replace("&amp;", "&");
                seen.add(u);
            }
        }
        log.debug("Parsed {} image URLs from GlobalComix page", seen.size());
        return new ArrayList<>(seen);
    }

    public String parseTitle(String html) {
        if (html == null || html.isBlank()) return "GlobalComix";
        Document doc = Jsoup.parse(html);
        Element ogTitle = doc.selectFirst("meta[property=og:title]");
        if (ogTitle != null) {
            String content = ogTitle.attr("content");
            if (content != null && !content.isBlank()) return content.trim();
        }
        String title = doc.title();
        if (title != null && !title.isBlank()) return title.trim();
        return "GlobalComix";
    }

    public long getRateLimitMs() {
        return RATE_LIMIT_MS;
    }

    public int getHttpTimeoutSeconds() {
        return HTTP_TIMEOUT_SECONDS;
    }

    public String filenameForPage(int pageNumber, String imageUrl) {
        String ext = "jpg";
        if (imageUrl != null) {
            try {
                String path = URI.create(imageUrl).getPath();
                if (path != null && path.contains(".")) {
                    String e = path.substring(path.lastIndexOf('.') + 1).toLowerCase();
                    if (e.matches("png|jpeg|gif|webp")) ext = e;
                    else if (e.equals("jpg")) ext = "jpg";
                }
            } catch (Exception ignored) {
            }
        }
        return "page_" + pageNumber + "." + ext;
    }
}

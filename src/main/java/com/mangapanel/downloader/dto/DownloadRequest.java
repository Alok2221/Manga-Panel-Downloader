package com.mangapanel.downloader.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DownloadRequest {

    @NotBlank(message = "URL is required")
    @Pattern(regexp = "https?://(www\\.)?(mangadex\\.org|globalcomix\\.com|mangaplus\\.shueisha\\.co\\.jp)[^\\s]*",
            message = "URL must be a chapter page from MangaDex, GlobalComix, or MangaPlus")
    private String chapterUrl;
}

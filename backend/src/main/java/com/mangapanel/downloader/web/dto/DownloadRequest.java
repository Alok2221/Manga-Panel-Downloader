package com.mangapanel.downloader.web.dto;

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
    @Pattern(
            regexp = "(?i)^https?://.+/chapter/[a-f0-9-]{36}/?$",
            message = "URL must be a MangaDex chapter page (/chapter/{uuid})"
    )
    private String chapterUrl;
}

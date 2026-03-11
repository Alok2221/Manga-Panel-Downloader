package com.mangapanel.downloader.integration.translator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PanelImageDto {
    private Long id;
    @JsonProperty("image_base64")
    private String imageBase64;
    @JsonProperty("image_url")
    private String imageUrl;
}

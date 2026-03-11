package com.mangapanel.downloader.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PanelTextSegmentDto {
    private Long id;
    private Long panelId;
    private Integer sequenceIndex;
    private String sourceLanguage;
    private String targetLanguage;
    private String sourceText;
    private String translatedText;
    private Integer bboxX;
    private Integer bboxY;
    private Integer bboxW;
    private Integer bboxH;
}

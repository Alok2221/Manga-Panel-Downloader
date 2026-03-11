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
public class OcrSegmentDto {
    @JsonProperty("panel_id")
    private Long panelId;
    @JsonProperty("sequence_index")
    private Integer sequenceIndex;
    private String text;
    @JsonProperty("bbox_x")
    private Integer bboxX;
    @JsonProperty("bbox_y")
    private Integer bboxY;
    @JsonProperty("bbox_w")
    private Integer bboxW;
    @JsonProperty("bbox_h")
    private Integer bboxH;
}

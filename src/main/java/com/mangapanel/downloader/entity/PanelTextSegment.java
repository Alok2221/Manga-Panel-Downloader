package com.mangapanel.downloader.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "panel_text_segment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PanelTextSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "panel_id", nullable = false)
    private Panel panel;

    @Column(name = "sequence_index", nullable = false)
    private Integer sequenceIndex;

    @Column(name = "source_language", length = 10)
    private String sourceLanguage;

    @Column(name = "target_language", length = 10)
    private String targetLanguage;

    @Column(name = "source_text", columnDefinition = "TEXT", nullable = false)
    private String sourceText;

    @Column(name = "translated_text", columnDefinition = "TEXT")
    private String translatedText;

    @Column(name = "bbox_x")
    private Integer bboxX;

    @Column(name = "bbox_y")
    private Integer bboxY;

    @Column(name = "bbox_w")
    private Integer bboxW;

    @Column(name = "bbox_h")
    private Integer bboxH;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}


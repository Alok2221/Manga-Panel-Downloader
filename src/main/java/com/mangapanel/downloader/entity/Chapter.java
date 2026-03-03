package com.mangapanel.downloader.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chapter")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manga_id")
    private Manga manga;

    @Column(name = "chapter_number", precision = 5, scale = 2)
    private BigDecimal chapterNumber;

    @Column(length = 255)
    private String title;

    @Column(columnDefinition = "TEXT", unique = true)
    private String url;

    @Column(name = "total_panels")
    private Integer totalPanels;

    @Column(name = "downloaded_at")
    private Instant downloadedAt;

    @Column(length = 10)
    private String language;

    @Column(name = "source_chapter_id", length = 36)
    private String sourceChapterId;

    @Column(name = "volume", length = 20)
    private String volume;

    @OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("pageNumber ASC")
    @Builder.Default
    private List<Panel> panels = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (downloadedAt == null) {
            downloadedAt = Instant.now();
        }
    }
}

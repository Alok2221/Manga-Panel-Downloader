package com.mangapanel.downloader.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "panel")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Panel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chapter_id", insertable = false, updatable = false)
    private Long chapterId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "local_path", columnDefinition = "TEXT")
    private String localPath;

    @Column(name = "data", columnDefinition = "bytea")
    private byte[] data;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "format", length = 10)
    private String format;

    @OneToMany(mappedBy = "panel", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<PanelTextSegment> textSegments = new java.util.ArrayList<>();
}

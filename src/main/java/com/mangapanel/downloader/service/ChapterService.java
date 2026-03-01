package com.mangapanel.downloader.service;

import com.mangapanel.downloader.dto.ChapterDto;
import com.mangapanel.downloader.entity.Chapter;
import com.mangapanel.downloader.repository.ChapterRepository;
import com.mangapanel.downloader.repository.PanelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final PanelRepository panelRepository;

    public Optional<Chapter> findById(Long id) {
        return chapterRepository.findById(id);
    }

    public Optional<ChapterDto> findDtoById(Long id) {
        return chapterRepository.findById(id).map(this::toDto);
    }

    public Page<ChapterDto> findAll(Pageable pageable) {
        return chapterRepository.findAll(pageable).map(this::toDto);
    }

    public Page<ChapterDto> search(String title, BigDecimal chapterNum, Pageable pageable) {
        String titleParam = (title != null && !title.isBlank()) ? title.trim() : "";
        return chapterRepository.search(titleParam, chapterNum, pageable).map(this::toDto);
    }

    @Transactional
    public void deleteById(Long id) {
        chapterRepository.findById(id).ifPresent(ch -> {
            ch.getPanels().clear();
            chapterRepository.delete(ch);
        });
    }

    public ChapterDto toDto(Chapter c) {
        long count = panelRepository.countByChapterId(c.getId());
        return ChapterDto.builder()
                .id(c.getId())
                .mangaId(c.getManga() != null ? c.getManga().getId() : null)
                .mangaTitle(c.getManga() != null ? c.getManga().getTitle() : null)
                .chapterNumber(c.getChapterNumber())
                .title(c.getTitle())
                .url(c.getUrl())
                .totalPanels(c.getTotalPanels())
                .panelsDownloaded((int) count)
                .downloadedAt(c.getDownloadedAt())
                .language(c.getLanguage())
                .build();
    }
}

package com.mangapanel.downloader.service;

import com.mangapanel.downloader.dto.PanelDto;
import com.mangapanel.downloader.entity.Panel;
import com.mangapanel.downloader.repository.PanelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PanelService {

    private final PanelRepository panelRepository;

    public List<PanelDto> findByChapterId(Long chapterId) {
        return panelRepository.findByChapterIdOrderByPageNumberAsc(chapterId).stream()
                .map(this::toDto)
                .toList();
    }

    public Page<PanelDto> findByChapterId(Long chapterId, Pageable pageable) {
        return panelRepository.findByChapterIdOrderByPageNumberAsc(chapterId, pageable).map(this::toDto);
    }

    public Optional<Panel> findById(Long id) {
        return panelRepository.findById(id);
    }

    public Optional<byte[]> getImageBytes(Long panelId) {
        return panelRepository.findById(panelId)
                .filter(p -> p.getLocalPath() != null)
                .map(p -> {
                    try {
                        return Files.readAllBytes(Path.of(p.getLocalPath()));
                    } catch (IOException e) {
                        return null;
                    }
                });
    }

    public Optional<String> getContentType(Long panelId) {
        return panelRepository.findById(panelId)
                .map(p -> {
                    String format = p.getFormat() != null ? p.getFormat().toLowerCase() : "png";
                    return switch (format) {
                        case "jpg", "jpeg" -> "image/jpeg";
                        case "gif" -> "image/gif";
                        case "webp" -> "image/webp";
                        default -> "image/png";
                    };
                });
    }

    public PanelDto toDto(Panel p) {
        return PanelDto.builder()
                .id(p.getId())
                .chapterId(p.getChapter() != null ? p.getChapter().getId() : null)
                .pageNumber(p.getPageNumber())
                .imageUrl(p.getImageUrl())
                .localPath(p.getLocalPath())
                .width(p.getWidth())
                .height(p.getHeight())
                .fileSize(p.getFileSize())
                .format(p.getFormat())
                .build();
    }
}

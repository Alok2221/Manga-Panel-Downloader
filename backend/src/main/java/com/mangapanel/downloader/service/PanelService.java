package com.mangapanel.downloader.service;

import com.mangapanel.downloader.entity.Panel;
import com.mangapanel.downloader.repository.PanelRepository;
import com.mangapanel.downloader.web.dto.PanelDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PanelService {

    private final PanelRepository panelRepository;

    public List<PanelDto> findByChapterId(Long chapterId) {
        return panelRepository.findDtosByChapterId(chapterId);
    }

    public Page<PanelDto> findByChapterId(Long chapterId, Pageable pageable) {
        return panelRepository.findDtosByChapterId(chapterId, pageable);
    }

    public Optional<Panel> findById(Long id) {
        return panelRepository.findById(id);
    }

    public Optional<byte[]> getImageBytes(Long panelId) {
        return panelRepository.findDataById(panelId);
    }

    public Optional<String> getContentType(Long panelId) {
        return panelRepository.findFormatById(panelId).map(fmt -> {
            String format = fmt != null ? fmt.toLowerCase() : "png";
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
                .chapterId(p.getChapterId())
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

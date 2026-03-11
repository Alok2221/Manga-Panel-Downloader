package com.mangapanel.downloader.repository;

import com.mangapanel.downloader.entity.Panel;
import com.mangapanel.downloader.web.dto.PanelDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PanelRepository extends JpaRepository<Panel, Long> {

    @Query("""
            SELECT NEW com.mangapanel.downloader.web.dto.PanelDto(
              p.id, p.chapterId, p.pageNumber, p.imageUrl, p.localPath, p.width, p.height, p.fileSize, p.format
            )
            FROM Panel p
            WHERE p.chapterId = :chapterId
            ORDER BY p.pageNumber ASC
            """)
    List<PanelDto> findDtosByChapterId(@Param("chapterId") Long chapterId);

    @Query("""
            SELECT NEW com.mangapanel.downloader.web.dto.PanelDto(
              p.id, p.chapterId, p.pageNumber, p.imageUrl, p.localPath, p.width, p.height, p.fileSize, p.format
            )
            FROM Panel p
            WHERE p.chapterId = :chapterId
            ORDER BY p.pageNumber ASC
            """)
    Page<PanelDto> findDtosByChapterId(@Param("chapterId") Long chapterId, Pageable pageable);

    @Query("SELECT p.data FROM Panel p WHERE p.id = :panelId")
    Optional<byte[]> findDataById(@Param("panelId") Long panelId);

    @Query("SELECT p.format FROM Panel p WHERE p.id = :panelId")
    Optional<String> findFormatById(@Param("panelId") Long panelId);

    long countByChapterId(Long chapterId);

    java.util.List<Panel> findByChapterIdOrderByPageNumberAsc(Long chapterId);
}


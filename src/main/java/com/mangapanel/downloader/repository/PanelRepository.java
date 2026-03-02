package com.mangapanel.downloader.repository;

import com.mangapanel.downloader.dto.PanelDto;
import com.mangapanel.downloader.entity.Panel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

import java.util.List;

public interface PanelRepository extends JpaRepository<Panel, Long> {

    @Query("""
            select new com.mangapanel.downloader.dto.PanelDto(
              p.id, p.chapterId, p.pageNumber, p.imageUrl, p.localPath, p.width, p.height, p.fileSize, p.format
            )
            from Panel p
            where p.chapterId = :chapterId
            order by p.pageNumber asc
            """)
    List<PanelDto> findDtosByChapterId(@Param("chapterId") Long chapterId);

    @Query("""
            select new com.mangapanel.downloader.dto.PanelDto(
              p.id, p.chapterId, p.pageNumber, p.imageUrl, p.localPath, p.width, p.height, p.fileSize, p.format
            )
            from Panel p
            where p.chapterId = :chapterId
            order by p.pageNumber asc
            """)
    Page<PanelDto> findDtosByChapterId(@Param("chapterId") Long chapterId, Pageable pageable);

    @Query("select p.data from Panel p where p.id = :panelId")
    Optional<byte[]> findDataById(@Param("panelId") Long panelId);

    @Query("select p.format from Panel p where p.id = :panelId")
    Optional<String> findFormatById(@Param("panelId") Long panelId);

    long countByChapterId(Long chapterId);
}

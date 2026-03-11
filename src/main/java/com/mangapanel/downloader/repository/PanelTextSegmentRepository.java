package com.mangapanel.downloader.repository;

import com.mangapanel.downloader.entity.PanelTextSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PanelTextSegmentRepository extends JpaRepository<PanelTextSegment, Long> {

    @Query("SELECT s FROM PanelTextSegment s JOIN s.panel p WHERE p.chapterId = :chapterId ORDER BY p.pageNumber ASC, s.sequenceIndex ASC")
    List<PanelTextSegment> findByChapterIdOrderByPanelAndSequence(@Param("chapterId") Long chapterId);

    @Query("SELECT s FROM PanelTextSegment s WHERE s.panel.id IN :panelIds ORDER BY s.sequenceIndex ASC")
    List<PanelTextSegment> findByPanelIds(@Param("panelIds") List<Long> panelIds);

    void deleteByPanel_IdIn(List<Long> panelIds);
}


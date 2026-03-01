package com.mangapanel.downloader.repository;

import com.mangapanel.downloader.entity.Panel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PanelRepository extends JpaRepository<Panel, Long> {

    List<Panel> findByChapterIdOrderByPageNumberAsc(Long chapterId);

    Page<Panel> findByChapterIdOrderByPageNumberAsc(Long chapterId, Pageable pageable);

    long countByChapterId(Long chapterId);
}

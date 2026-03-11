package com.mangapanel.downloader.repository;

import com.mangapanel.downloader.entity.Manga;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MangaRepository extends JpaRepository<Manga, Long> {

    List<Manga> findByTitleContainingIgnoreCase(String title);
}


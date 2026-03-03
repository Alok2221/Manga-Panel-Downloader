package com.mangapanel.downloader.repository;

import com.mangapanel.downloader.entity.Chapter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    List<Chapter> findAllByOrderByIdAsc();

    Optional<Chapter> findByUrl(String url);

    boolean existsByUrl(String url);

    List<Chapter> findByMangaIdOrderByChapterNumberAsc(Long mangaId);

    @Query("SELECT c FROM Chapter c JOIN c.manga m WHERE LOWER(m.title) LIKE LOWER(CONCAT('%', :title, '%'))")
    Page<Chapter> searchByMangaTitle(@Param("title") String title, Pageable pageable);

    @Query("SELECT c FROM Chapter c JOIN c.manga m WHERE (:title IS NULL OR :title = '' OR LOWER(m.title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
            "AND (:chapterNum IS NULL OR c.chapterNumber = :chapterNum)")
    Page<Chapter> search(@Param("title") String title,
                         @Param("chapterNum") java.math.BigDecimal chapterNum,
                         Pageable pageable);

    @Query("SELECT c FROM Chapter c JOIN FETCH c.manga m " +
            "WHERE (:title IS NULL OR :title = '' OR LOWER(m.title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
            "AND (:chapterNum IS NULL OR c.chapterNumber = :chapterNum) " +
            "AND (:volume IS NULL OR :volume = '' OR LOWER(COALESCE(c.volume, 'none')) LIKE LOWER(CONCAT('%', :volume, '%'))) " +
            "ORDER BY m.title, c.volume NULLS LAST, c.chapterNumber NULLS LAST, c.id")
    List<Chapter> findForGrouped(@Param("title") String title,
                                 @Param("chapterNum") java.math.BigDecimal chapterNum,
                                 @Param("volume") String volume);

    @Query("SELECT c FROM Chapter c JOIN FETCH c.manga m " +
            "WHERE LOWER(m.title) = LOWER(:title) " +
            "ORDER BY c.volume NULLS LAST, c.chapterNumber NULLS LAST, c.id")
    List<Chapter> findByMangaTitleExact(@Param("title") String title);
}

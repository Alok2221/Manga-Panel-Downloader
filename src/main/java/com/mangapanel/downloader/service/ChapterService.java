package com.mangapanel.downloader.service;

import com.mangapanel.downloader.entity.Chapter;
import com.mangapanel.downloader.repository.ChapterRepository;
import com.mangapanel.downloader.repository.PanelRepository;
import com.mangapanel.downloader.web.dto.ChapterDto;
import com.mangapanel.downloader.web.dto.ChapterGroupedDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final PanelRepository panelRepository;
    private final JdbcTemplate jdbcTemplate;

    private static final long REINDEX_OFFSET = 100_000L;

    public Optional<Chapter> findById(Long id) {
        return chapterRepository.findById(id);
    }

    public Optional<ChapterDto> findDtoById(Long id) {
        return chapterRepository.findById(id).map(this::toDto);
    }

    public Page<ChapterDto> findAll(Pageable pageable) {
        return chapterRepository.findAll(pageable).map(this::toDto);
    }

    public Page<ChapterDto> search(String title, BigDecimal chapterNum, String volume, Pageable pageable) {
        String titleParam = (title != null && !title.isBlank()) ? title.trim() : "";
        String volumeParam = (volume != null && !volume.isBlank()) ? volume.trim() : "";
        return chapterRepository.search(titleParam, chapterNum, volumeParam, pageable).map(this::toDto);
    }

    public List<ChapterGroupedDto> findGroupedByMangaAndVolume(String title, BigDecimal chapterNum, String volume) {
        String titleParam = (title != null && !title.isBlank()) ? title.trim() : null;
        String volumeParam = (volume != null && !volume.isBlank()) ? volume.trim() : null;
        List<Chapter> chapters = chapterRepository.findForGrouped(titleParam, chapterNum, volumeParam);
        Map<String, Map<String, List<ChapterDto>>> byMangaThenVolume = new LinkedHashMap<>();
        for (Chapter c : chapters) {
            String mangaKey = c.getManga() != null ? c.getManga().getTitle() : "Unknown";
            if (mangaKey == null) mangaKey = "Unknown";
            String volKey = c.getVolume() != null && !c.getVolume().isBlank() ? c.getVolume() : "none";
            byMangaThenVolume
                    .computeIfAbsent(mangaKey, k -> new LinkedHashMap<>())
                    .computeIfAbsent(volKey, k -> new ArrayList<>())
                    .add(toDto(c));
        }
        List<ChapterGroupedDto> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<ChapterDto>>> mangaEntry : byMangaThenVolume.entrySet()) {
            List<ChapterGroupedDto.VolumeGroupDto> volumes = new ArrayList<>();
            List<String> volumeKeys = new ArrayList<>(mangaEntry.getValue().keySet());

            volumeKeys.sort(ChapterService::compareVolumeKeys);
            for (String volKey : volumeKeys) {
                List<ChapterDto> chapterDtos = mangaEntry.getValue().get(volKey);
                if (chapterDtos != null) {
                    chapterDtos.sort((c1, c2) -> compareChapterNumbers(c1.getChapterNumber(), c2.getChapterNumber(), c1.getId(), c2.getId()));
                }
                volumes.add(ChapterGroupedDto.VolumeGroupDto.builder()
                        .volume(volKey)
                        .chapters(chapterDtos)
                        .build());
            }
            Long mangaId = mangaEntry.getValue().values().stream()
                    .flatMap(List::stream)
                    .findFirst()
                    .map(ChapterDto::getMangaId)
                    .orElse(null);
            String coverUrl = chapters.stream()
                    .filter(c -> c.getManga() != null && mangaEntry.getKey().equals(c.getManga().getTitle()))
                    .map(c -> c.getManga().getCoverUrl())
                    .filter(u -> u != null && !u.isBlank())
                    .findFirst()
                    .orElse(null);
            result.add(ChapterGroupedDto.builder()
                    .mangaId(mangaId)
                    .mangaTitle(mangaEntry.getKey())
                    .mangaCoverUrl(coverUrl)
                    .volumes(volumes)
                    .build());
        }
        return result;
    }

    private static final Pattern NUMERIC = Pattern.compile("^\\d+(?:\\.\\d+)?$");

    private static int compareVolumeKeys(String a, String b) {
        String va = (a == null || a.isBlank()) ? "none" : a;
        String vb = (b == null || b.isBlank()) ? "none" : b;
        if ("none".equalsIgnoreCase(va) && "none".equalsIgnoreCase(vb)) return 0;
        if ("none".equalsIgnoreCase(va)) return 1;
        if ("none".equalsIgnoreCase(vb)) return -1;

        BigDecimal na = parseNumericOrNull(va);
        BigDecimal nb = parseNumericOrNull(vb);
        if (na != null && nb != null) return na.compareTo(nb);
        if (na != null) return -1;
        if (nb != null) return 1;
        return va.compareToIgnoreCase(vb);
    }

    private static int compareChapterNumbers(BigDecimal a, BigDecimal b, Long idA, Long idB) {
        int compare = Long.compare(idA != null ? idA : 0L, idB != null ? idB : 0L);
        if (a == null && b == null) return compare;
        if (a == null) return 1;
        if (b == null) return -1;
        int cmp = a.compareTo(b);
        if (cmp != 0) return cmp;
        return compare;
    }

    private static BigDecimal parseNumericOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (!NUMERIC.matcher(t).matches()) return null;
        try {
            return new BigDecimal(t);
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional
    public void deleteById(Long id) {
        chapterRepository.findById(id).ifPresent(ch -> {
            ch.getPanels().clear();
            chapterRepository.delete(ch);
        });
    }

    @Transactional
    public void reindexChapters() {
        List<Chapter> ordered = chapterRepository.findAllByOrderByIdAsc();
        if (ordered.isEmpty()) {
            resetChapterSequence(0L);
            return;
        }
        List<Long> oldIds = ordered.stream().map(Chapter::getId).toList();
        for (int i = 0; i < oldIds.size(); i++) {
            long oldId = oldIds.get(i);
            long newId = i + 1L;
            if (oldId == newId) continue;
            long tempId = oldId + REINDEX_OFFSET;
            jdbcTemplate.update("UPDATE panel SET chapter_id = ? WHERE chapter_id = ?", tempId, oldId);
            jdbcTemplate.update("UPDATE chapter SET id = ? WHERE id = ?", tempId, oldId);
        }
        for (int i = 0; i < oldIds.size(); i++) {
            long oldId = oldIds.get(i);
            long newId = i + 1L;
            if (oldId == newId) continue;
            long tempId = oldId + REINDEX_OFFSET;
            jdbcTemplate.update("UPDATE panel SET chapter_id = ? WHERE chapter_id = ?", newId, tempId);
            jdbcTemplate.update("UPDATE chapter SET id = ? WHERE id = ?", newId, tempId);
        }
        resetChapterSequence((long) ordered.size());
    }

    private void resetChapterSequence(Long maxId) {
        try {
            jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('chapter', 'id'), " + Math.max(1, maxId) + ")");
        } catch (Exception e) {
            try {
                jdbcTemplate.execute("SELECT setval('chapter_id_seq', " + Math.max(1, maxId) + ")");
            } catch (Exception ignored) {
            }
        }
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
                .volume(c.getVolume())
                .scanlationGroup(c.getScanlationGroup())
                .build();
    }

    /**
     * Chapter navigation list for a manga title (exact match, case-insensitive).
     * Sorted by volume (numeric), chapter number (numeric), then id.
     */
    public List<ChapterDto> getChapterSequenceForMangaTitle(String mangaTitle) {
        if (mangaTitle == null || mangaTitle.isBlank()) return List.of();
        List<Chapter> chapters = chapterRepository.findByMangaTitleExact(mangaTitle.trim());
        List<ChapterDto> dtos = chapters.stream().map(this::toDto).toList();
        List<ChapterDto> sorted = new ArrayList<>(dtos);
        sorted.sort((a, b) -> {
            int vCmp = compareVolumeKeys(a.getVolume(), b.getVolume());
            if (vCmp != 0) return vCmp;
            return compareChapterNumbers(a.getChapterNumber(), b.getChapterNumber(), a.getId(), b.getId());
        });
        return sorted;
    }
}

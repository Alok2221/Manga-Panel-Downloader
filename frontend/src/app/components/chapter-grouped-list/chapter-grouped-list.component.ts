import { Component, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import JSZip from 'jszip';
import { ApiService } from '../../services/api.service';
import { Chapter, ChapterGrouped } from '../../models/chapter.model';

@Component({
  selector: 'app-chapter-grouped-list',
  standalone: true,
  imports: [RouterLink, DatePipe],
  templateUrl: './chapter-grouped-list.component.html',
  styleUrl: './chapter-grouped-list.component.scss'
})
export class ChapterGroupedListComponent {
  grouped = input.required<ChapterGrouped[]>();
  loading = input<boolean>(false);
  downloadingZip = signal<number | null>(null);

  expandedManga = signal<Set<string>>(new Set());
  expandedVolume = signal<Set<string>>(new Set());

  constructor(private api: ApiService) {}

  toggleManga(key: string): void {
    const set = new Set(this.expandedManga());
    if (set.has(key)) set.delete(key);
    else set.add(key);
    this.expandedManga.set(set);
  }

  toggleVolume(key: string): void {
    const set = new Set(this.expandedVolume());
    if (set.has(key)) set.delete(key);
    else set.add(key);
    this.expandedVolume.set(set);
  }

  isMangaExpanded(key: string): boolean {
    return this.expandedManga().has(key);
  }

  isVolumeExpanded(key: string): boolean {
    return this.expandedVolume().has(key);
  }

  mangaKey(group: ChapterGrouped): string {
    return `${group.mangaId ?? 0}-${group.mangaTitle}`;
  }

  volumeKey(mangaKey: string, volume: string): string {
    return `${mangaKey}::${volume}`;
  }

  async downloadChapterZip(ch: Chapter, event: Event): Promise<void> {
    event.preventDefault();
    event.stopPropagation();
    if (this.downloadingZip() !== null) return;
    this.downloadingZip.set(ch.id);
    try {
      const panels = await firstValueFrom(this.api.getPanels(ch.id));
      if (!panels?.length) {
        this.downloadingZip.set(null);
        return;
      }
      const zip = new JSZip();
      for (const panel of panels.sort((a, b) => a.pageNumber - b.pageNumber)) {
        const url = this.api.getPanelImageUrl(panel.id);
        const ext = panel.format || 'png';
        const name = `page-${String(panel.pageNumber).padStart(3, '0')}.${ext}`;
        const blob = await fetch(url).then((r) => r.blob());
        zip.file(name, blob);
      }
      const safeTitle = (ch.mangaTitle ?? 'Manga').replace(/[<>:"/\\|?*]/g, '_').slice(0, 80);
      const chNum = ch.chapterNumber != null ? String(ch.chapterNumber) : ch.id;
      const zipName = `${safeTitle} - Ch.${chNum}.zip`;
      const content = await zip.generateAsync({ type: 'blob' });
      const a = document.createElement('a');
      a.href = URL.createObjectURL(content);
      a.download = zipName;
      a.click();
      URL.revokeObjectURL(a.href);
    } finally {
      this.downloadingZip.set(null);
    }
  }

  progressPercent(ch: Chapter): number {
    const total = ch.totalPanels ?? 0;
    const done = ch.panelsDownloaded ?? 0;
    if (total <= 0) return 100;
    return Math.round((done / total) * 100);
  }

  isDownloading(ch: Chapter): boolean {
    const total = ch.totalPanels ?? 0;
    const done = ch.panelsDownloaded ?? 0;
    return total > 0 && done < total;
  }
}

import { Component, input, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import JSZip from 'jszip';
import { ApiService } from '../../services/api.service';
import { Chapter, ChapterGrouped, VolumeGroup } from '../../models/chapter.model';

@Component({
  selector: 'app-manga-cards-grid',
  standalone: true,
  templateUrl: './manga-cards-grid.component.html',
  styleUrl: './manga-cards-grid.component.scss'
})
export class MangaCardsGridComponent {
  grouped = input.required<ChapterGrouped[]>();
  loading = input<boolean>(false);

  downloadingZip = signal<number | null>(null);
  downloadingManga = signal<string | null>(null);
  downloadingVolume = signal<string | null>(null);
  expandedVolume = signal<Set<string>>(new Set());

  private readonly coverGradients = [
    'linear-gradient(135deg, rgba(34, 197, 94, 0.4) 0%, rgba(139, 92, 246, 0.4) 100%)',
    'linear-gradient(135deg, rgba(248, 113, 163, 0.35) 0%, rgba(244, 63, 94, 0.35) 100%)',
    'linear-gradient(135deg, rgba(56, 189, 248, 0.4) 0%, rgba(34, 211, 238, 0.4) 100%)',
    'linear-gradient(135deg, rgba(34, 197, 94, 0.35) 0%, rgba(52, 211, 153, 0.35) 100%)',
    'linear-gradient(135deg, rgba(250, 204, 21, 0.3) 0%, rgba(251, 146, 60, 0.3) 100%)'
  ];

  constructor(
    private api: ApiService,
    private router: Router
  ) {}

  coverStyle(index: number): { background: string } {
    const i = index % this.coverGradients.length;
    return { background: this.coverGradients[i] };
  }

  mangaKey(group: ChapterGrouped): string {
    return `${group.mangaId ?? 0}-${group.mangaTitle}`;
  }

  volumeKey(mangaKey: string, volume: string): string {
    return `${mangaKey}::${volume}`;
  }

  totalChapters(group: ChapterGrouped): number {
    return group.volumes.reduce((sum, v) => sum + v.chapters.length, 0);
  }

  firstChapterId(group: ChapterGrouped): number | null {
    for (const vol of group.volumes) {
      if (vol.chapters.length > 0) return vol.chapters[0].id;
    }
    return null;
  }

  firstChapterIdInVolume(vol: VolumeGroup): number | null {
    return vol.chapters.length > 0 ? vol.chapters[0].id : null;
  }

  toggleVolume(key: string): void {
    const set = new Set(this.expandedVolume());
    if (set.has(key)) set.delete(key);
    else set.add(key);
    this.expandedVolume.set(set);
  }

  isVolumeExpanded(key: string): boolean {
    return this.expandedVolume().has(key);
  }

  readChapter(ch: Chapter): void {
    this.router.navigate(['/chapters', ch.id]);
  }

  readAll(group: ChapterGrouped): void {
    const id = this.firstChapterId(group);
    if (id != null) {
      this.router.navigate(['/chapters', id], { queryParams: { mangaTitle: group.mangaTitle } });
    }
  }

  readVolume(vol: VolumeGroup): void {
    const id = this.firstChapterIdInVolume(vol);
    if (id != null) {
      // mangaTitle will be inferred in reader if not provided; kept simple here.
      this.router.navigate(['/chapters', id]);
    }
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
        const blob = await fetch(this.api.getPanelImageUrl(panel.id)).then((r) => r.blob());
        const ext = panel.format || 'png';
        zip.file(`page-${String(panel.pageNumber).padStart(3, '0')}.${ext}`, blob);
      }
      const safeTitle = (ch.mangaTitle ?? 'Manga').replace(/[<>:"/\\|?*]/g, '_').slice(0, 80);
      const chNum = ch.chapterNumber != null ? String(ch.chapterNumber) : ch.id;
      const content = await zip.generateAsync({ type: 'blob' });
      const a = document.createElement('a');
      a.href = URL.createObjectURL(content);
      a.download = `${safeTitle} - Ch.${chNum}.zip`;
      a.click();
      URL.revokeObjectURL(a.href);
    } finally {
      this.downloadingZip.set(null);
    }
  }

  async downloadVolumeZip(vol: VolumeGroup, mangaTitle: string, vk: string, event: Event): Promise<void> {
    event.preventDefault();
    event.stopPropagation();
    if (this.downloadingVolume() !== null) return;
    this.downloadingVolume.set(vk);
    try {
      const zip = new JSZip();
      const safeTitle = (mangaTitle ?? 'Manga').replace(/[<>:"/\\|?*]/g, '_').slice(0, 60);
      const volLabel = vol.volume === 'none' ? 'none' : vol.volume;
      for (const ch of vol.chapters) {
        const panels = await firstValueFrom(this.api.getPanels(ch.id));
        if (!panels?.length) continue;
        const chNum = ch.chapterNumber != null ? String(ch.chapterNumber) : ch.id;
        const folder = `ch-${chNum}`;
        for (const panel of panels.sort((a, b) => a.pageNumber - b.pageNumber)) {
          const blob = await fetch(this.api.getPanelImageUrl(panel.id)).then((r) => r.blob());
          const ext = panel.format || 'png';
          zip.file(`${folder}/page-${String(panel.pageNumber).padStart(3, '0')}.${ext}`, blob);
        }
      }
      const content = await zip.generateAsync({ type: 'blob' });
      const a = document.createElement('a');
      a.href = URL.createObjectURL(content);
      a.download = `${safeTitle} - Vol.${volLabel}.zip`;
      a.click();
      URL.revokeObjectURL(a.href);
    } finally {
      this.downloadingVolume.set(null);
    }
  }

  async downloadMangaAll(group: ChapterGrouped, event: Event): Promise<void> {
    event.preventDefault();
    event.stopPropagation();
    const mk = this.mangaKey(group);
    if (this.downloadingManga() !== null) return;
    this.downloadingManga.set(mk);
    try {
      const zip = new JSZip();
      const safeTitle = (group.mangaTitle ?? 'Manga').replace(/[<>:"/\\|?*]/g, '_').slice(0, 60);
      for (const vol of group.volumes) {
        for (const ch of vol.chapters) {
          const panels = await firstValueFrom(this.api.getPanels(ch.id));
          if (!panels?.length) continue;
          const chNum = ch.chapterNumber != null ? String(ch.chapterNumber) : ch.id;
          const folder = `ch-${chNum}`;
          for (const panel of panels.sort((a, b) => a.pageNumber - b.pageNumber)) {
            const blob = await fetch(this.api.getPanelImageUrl(panel.id)).then((r) => r.blob());
            const ext = panel.format || 'png';
            zip.file(`${folder}/page-${String(panel.pageNumber).padStart(3, '0')}.${ext}`, blob);
          }
        }
      }
      const content = await zip.generateAsync({ type: 'blob' });
      const a = document.createElement('a');
      a.href = URL.createObjectURL(content);
      a.download = `${safeTitle} - All.zip`;
      a.click();
      URL.revokeObjectURL(a.href);
    } finally {
      this.downloadingManga.set(null);
    }
  }
}

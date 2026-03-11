import { Component, signal, OnInit, input, effect } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import JSZip from 'jszip';
import { ApiService } from '../../services/api.service';
import { Chapter, ChapterPage } from '../../models/chapter.model';

@Component({
  selector: 'app-chapter-grid',
  standalone: true,
  imports: [RouterLink, DatePipe],
  templateUrl: './chapter-grid.component.html',
  styleUrl: './chapter-grid.component.scss'
})
export class ChapterGridComponent implements OnInit {
  chapters = signal<Chapter[]>([]);
  totalPages = signal(0);
  currentPage = signal(0);
  loading = signal(true);
  searchTitle = input<string>('');
  searchChapter = input<number | undefined>(undefined);
  searchVolume = input<string | undefined>(undefined);
  refreshTrigger = input<number>(0);
  downloadingZip = signal<number | null>(null);

  constructor(private api: ApiService) {
    effect(() => {
      this.refreshTrigger();
      this.searchTitle();
      this.searchChapter();
      this.searchVolume();
      this.load(0);
    });
  }

  ngOnInit(): void {}

  load(page = 0): void {
    this.loading.set(true);
    const title = this.searchTitle();
    const ch = this.searchChapter();
    const vol = this.searchVolume();
    (title || ch != null || (vol != null && vol !== '')
      ? this.api.search(title, ch, vol, page, 24)
      : this.api.getChapters(page, 24)
    ).subscribe({
      next: (p: ChapterPage) => {
        this.chapters.set(p.content || []);
        this.totalPages.set(p.totalPages ?? 0);
        this.currentPage.set(p.number ?? 0);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
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

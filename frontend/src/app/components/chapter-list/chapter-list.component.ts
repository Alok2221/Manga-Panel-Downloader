import { Component, signal, OnInit, input, output, effect } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { Chapter, ChapterPage } from '../../models/chapter.model';

@Component({
  selector: 'app-chapter-list',
  standalone: true,
  imports: [DatePipe, RouterLink],
  templateUrl: './chapter-list.component.html',
  styleUrl: './chapter-list.component.scss'
})
export class ChapterListComponent implements OnInit {
  chapters = signal<Chapter[]>([]);
  totalElements = signal(0);
  totalPages = signal(0);
  currentPage = signal(0);
  loading = signal(true);
  searchTitle = input<string>('');
  searchChapter = input<number | undefined>(undefined);
  searchVolume = input<string | undefined>(undefined);
  refreshTrigger = input<number>(0);
  chapterDeleted = output<number>();

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
      ? this.api.search(title, ch, vol, page, 20)
      : this.api.getChapters(page, 20)
    ).subscribe({
      next: (p: ChapterPage) => {
        this.chapters.set(p.content || []);
        this.totalElements.set(p.totalElements ?? 0);
        this.totalPages.set(p.totalPages ?? 0);
        this.currentPage.set(p.number ?? 0);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  deleteChapter(id: number, event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    if (!confirm('Delete this chapter and all its panels?')) return;
    this.api.deleteChapter(id).subscribe({
      next: () => {
        this.chapterDeleted.emit(id);
        this.load(this.currentPage());
      }
    });
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

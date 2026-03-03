import { Component, OnInit, signal, effect } from '@angular/core';
import { Router } from '@angular/router';
import { MangaCardsGridComponent } from '../components/manga-cards-grid/manga-cards-grid.component';
import { Chapter } from '../models/chapter.model';
import { ChapterGrouped } from '../models/chapter.model';
import { ApiService } from '../services/api.service';

@Component({
  selector: 'app-read-manga-page',
  standalone: true,
  imports: [MangaCardsGridComponent],
  templateUrl: './read-manga-page.component.html',
  styleUrl: './read-manga-page.component.scss'
})
export class ReadMangaPageComponent implements OnInit {
  searchTitle = signal<string | null>(null);
  searchChapter = signal<number | null>(null);
  searchVolume = signal<string | null>(null);
  lastReadChapter = signal<Chapter | null>(null);
  refreshTrigger = signal(0);
  reindexing = signal(false);
  filtersOpen = signal(false);
  grouped = signal<ChapterGrouped[]>([]);
  loading = signal(false);

  constructor(
    private api: ApiService,
    private router: Router
  ) {
    effect(() => {
      this.refreshTrigger();
      this.searchTitle();
      this.searchChapter();
      this.searchVolume();
      this.loadGrouped();
    });
  }

  ngOnInit(): void {
    const lastIdRaw = localStorage.getItem('manga:lastChapterId');
    if (lastIdRaw) {
      const id = Number(lastIdRaw);
      if (id) {
        this.api.getChapter(id).subscribe({
          next: (ch) => this.lastReadChapter.set(ch)
        });
      }
    }
  }

  private loadGrouped(): void {
    this.loading.set(true);
    this.api.getChaptersGrouped(
      this.searchTitle(),
      this.searchChapter(),
      this.searchVolume()
    ).subscribe({
      next: (data) => {
        this.grouped.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  onTitleChange(value: string): void {
    const title = value.trim();
    this.searchTitle.set(title || null);
    this.refreshTrigger.update((v) => v + 1);
  }

  onChapterChange(value: string): void {
    const s = value.trim();
    if (s === '') {
      this.searchChapter.set(null);
    } else {
      const n = parseFloat(s);
      this.searchChapter.set(Number.isNaN(n) ? null : n);
    }
    this.refreshTrigger.update((v) => v + 1);
  }

  onVolumeChange(value: string): void {
    const vol = value.trim();
    this.searchVolume.set(vol || null);
    this.refreshTrigger.update((v) => v + 1);
  }

  toggleFilters(): void {
    this.filtersOpen.update((v) => !v);
  }

  onOpenChapter(ch: Chapter): void {
    localStorage.setItem('manga:lastChapterId', String(ch.id));
    this.router.navigate(['/chapters', ch.id]);
  }

  onReindex(): void {
    if (this.reindexing()) return;
    this.reindexing.set(true);
    this.api.reindexChapters().subscribe({
      next: () => {
        this.reindexing.set(false);
        this.refreshTrigger.update((v) => v + 1);
      },
      error: () => this.reindexing.set(false)
    });
  }
}


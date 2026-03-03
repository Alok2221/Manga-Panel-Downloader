import { Component, signal, OnInit, HostListener, ElementRef, ViewChild } from '@angular/core';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { Chapter } from '../../models/chapter.model';
import { Panel } from '../../models/panel.model';

@Component({
  selector: 'app-panel-gallery',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './panel-gallery.component.html',
  styleUrl: './panel-gallery.component.scss'
})
export class PanelGalleryComponent implements OnInit {
  @ViewChild('fullscreenTarget') fullscreenTarget!: ElementRef<HTMLDivElement>;

  chapter = signal<Chapter | null>(null);
  panels = signal<Panel[]>([]);
  loading = signal(true);
  currentIndex = signal(0);
  isFullscreen = signal(false);
  mangaSequence = signal<Chapter[]>([]);
  sequenceLoading = signal(false);
  startAtEnd = signal(false);

  constructor(
    private api: ApiService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    if (typeof document !== 'undefined') {
      document.addEventListener('fullscreenchange', () => this.isFullscreen.set(!!document.fullscreenElement));
    }
  }

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((qp) => {
      this.startAtEnd.set(qp.get('start') === 'last');
    });

    this.route.paramMap.subscribe((pm) => {
      const id = Number(pm.get('id'));
      if (!id) {
        this.loading.set(false);
        return;
      }
      this.loadChapter(id);
    });
  }

  private loadChapter(id: number): void {
    this.loading.set(true);
    this.currentIndex.set(0);
    this.panels.set([]);
    this.chapter.set(null);

    this.api.getChapter(id).subscribe({
      next: (c) => {
        this.chapter.set(c);
        const title = c.mangaTitle ?? this.route.snapshot.queryParamMap.get('mangaTitle');
        if (title) this.loadSequence(title);
      },
      error: () => this.loading.set(false)
    });

    this.api.getPanels(id).subscribe({
      next: (p) => {
        const sorted = p.sort((a, b) => a.pageNumber - b.pageNumber);
        this.panels.set(sorted);
        if (this.startAtEnd() && sorted.length > 0) {
          this.currentIndex.set(sorted.length - 1);
          this.startAtEnd.set(false);
        }
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  private loadSequence(mangaTitle: string): void {
    this.sequenceLoading.set(true);
    this.api.getChapterSequence(mangaTitle).subscribe({
      next: (seq) => {
        this.mangaSequence.set(seq || []);
        this.sequenceLoading.set(false);
      },
      error: () => this.sequenceLoading.set(false)
    });
  }

  currentPanel(): Panel | null {
    const list = this.panels();
    const i = this.currentIndex();
    if (list.length === 0 || i < 0 || i >= list.length) return null;
    return list[i];
  }

  panelImageUrl(panelId: number): string {
    return this.api.getPanelImageUrl(panelId);
  }

  prev(): void {
    const i = this.currentIndex();
    if (i > 0) {
      this.currentIndex.set(i - 1);
      return;
    }
    const prevChapter = this.prevChapterId();
    if (prevChapter != null) {
      this.router.navigate(['/chapters', prevChapter], { queryParams: { start: 'last' } });
    }
  }

  next(): void {
    const i = this.currentIndex();
    const lastIndex = this.panels().length - 1;
    if (i < lastIndex) {
      this.currentIndex.set(i + 1);
      return;
    }
    const nextChapter = this.nextChapterId();
    if (nextChapter != null) {
      this.router.navigate(['/chapters', nextChapter]);
    }
  }

  prevChapterId(): number | null {
    const ch = this.chapter();
    if (!ch) return null;
    const seq = this.mangaSequence();
    const idx = seq.findIndex((c) => c.id === ch.id);
    if (idx <= 0) return null;
    return seq[idx - 1].id;
  }

  nextChapterId(): number | null {
    const ch = this.chapter();
    if (!ch) return null;
    const seq = this.mangaSequence();
    const idx = seq.findIndex((c) => c.id === ch.id);
    if (idx < 0 || idx >= seq.length - 1) return null;
    return seq[idx + 1].id;
  }

  openPrevChapter(): void {
    const id = this.prevChapterId();
    if (id != null) this.router.navigate(['/chapters', id], { queryParams: { start: 'last' } });
  }

  openNextChapter(): void {
    const id = this.nextChapterId();
    if (id != null) this.router.navigate(['/chapters', id]);
  }

  toggleFullscreen(): void {
    const el = this.fullscreenTarget?.nativeElement;
    if (!el) return;
    if (!document.fullscreenElement) {
      el.requestFullscreen?.();
    } else {
      document.exitFullscreen?.();
    }
  }

  exitFullscreen(): void {
    if (document.fullscreenElement) {
      document.exitFullscreen?.();
    }
  }

  @HostListener('document:keydown', ['$event'])
  onKeyDown(e: KeyboardEvent): void {
    if (e.key === 'ArrowLeft') this.prev();
    if (e.key === 'ArrowRight') this.next();
    if (e.key === 'Escape') this.exitFullscreen();
  }
}

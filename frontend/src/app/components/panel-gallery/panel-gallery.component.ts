import { Component, signal, OnInit, HostListener, ElementRef, ViewChild, computed } from '@angular/core';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { Chapter } from '../../models/chapter.model';
import { Panel } from '../../models/panel.model';
import { PanelTextSegment } from '../../models/panel-text-segment.model';

const READER_LANG_PREF = 'readerLanguagePreference';
const READER_TAB_PREF = 'readerActiveTab';

type TextViewMode = 'original' | 'translated' | 'both';
type ReaderTabMode = 'pages' | 'translation';

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

  segments = signal<PanelTextSegment[]>([]);
  ocrLoading = signal(false);
  translateLoading = signal(false);
  textViewMode = signal<TextViewMode>('both');
  tabMode = signal<ReaderTabMode>('pages');

  currentPanelSegments = computed(() => {
    const panel = this.currentPanel();
    const segs = this.segments();
    if (!panel || segs.length === 0) return [];
    return segs.filter((s) => s.panelId === panel.id).sort((a, b) => a.sequenceIndex - b.sequenceIndex);
  });

  constructor(
    private api: ApiService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    if (typeof document !== 'undefined') {
      document.addEventListener('fullscreenchange', () => this.isFullscreen.set(!!document.fullscreenElement));

      const savedTab = localStorage.getItem(READER_TAB_PREF) as ReaderTabMode | null;
      if (savedTab && (savedTab === 'pages' || savedTab === 'translation')) {
        this.tabMode.set(savedTab);
      }
    }
  }

  ngOnInit(): void {
    const saved = localStorage.getItem(READER_LANG_PREF) as TextViewMode | null;
    if (saved && ['original', 'translated', 'both'].includes(saved)) {
      this.textViewMode.set(saved);
    }

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
    this.segments.set([]);

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

    this.api.getChapterTexts(id).subscribe({
      next: (s) => this.segments.set(s || []),
      error: () => this.segments.set([])
    });
  }

  setTextViewMode(mode: TextViewMode): void {
    this.textViewMode.set(mode);
    localStorage.setItem(READER_LANG_PREF, mode);
  }

  setTabMode(mode: ReaderTabMode): void {
    this.tabMode.set(mode);
    try {
      localStorage.setItem(READER_TAB_PREF, mode);
    } catch {
      // ignore storage errors in restricted environments
    }
  }

  startOcr(): void {
    const ch = this.chapter();
    if (!ch || this.ocrLoading()) return;
    this.ocrLoading.set(true);
    this.api.startChapterOcr(ch.id).subscribe({
      next: () => {
        this.api.getChapterTexts(ch.id).subscribe({
          next: (s) => this.segments.set(s || []),
          error: () => {}
        });
        this.ocrLoading.set(false);
      },
      error: () => this.ocrLoading.set(false)
    });
  }

  startTranslation(): void {
    const ch = this.chapter();
    if (!ch || this.translateLoading()) return;
    this.translateLoading.set(true);
    this.api.startChapterTranslation(ch.id).subscribe({
      next: () => {
        this.api.getChapterTexts(ch.id).subscribe({
          next: (s) => this.segments.set(s || []),
          error: () => {}
        });
        this.translateLoading.set(false);
      },
      error: () => this.translateLoading.set(false)
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

  onImageClick(event: MouseEvent): void {
    const target = event.target as HTMLElement | null;
    const width = target?.clientWidth ?? 0;
    const clickX = (event as any).offsetX as number | undefined;

    if (!width || clickX === undefined) {
      this.next();
      return;
    }

    if (clickX < width / 2) {
      this.prev();
    } else {
      this.next();
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

import { Component, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ChapterListComponent } from '../components/chapter-list/chapter-list.component';
import { Chapter } from '../models/chapter.model';
import { ApiService } from '../services/api.service';

@Component({
  selector: 'app-read-manga-page',
  standalone: true,
  imports: [RouterLink, ChapterListComponent],
  templateUrl: './read-manga-page.component.html',
  styleUrl: './read-manga-page.component.scss'
})
export class ReadMangaPageComponent implements OnInit {
  highlightedMangaTitle = signal<string | null>(null);
  lastReadChapter = signal<Chapter | null>(null);
  refreshTrigger = signal(0);

  constructor(
    private api: ApiService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const lastIdRaw = localStorage.getItem('manga:lastChapterId');
    if (lastIdRaw) {
      const id = Number(lastIdRaw);
      if (id) {
        this.api.getChapter(id).subscribe({
          next: (ch) => {
            this.lastReadChapter.set(ch);
            this.highlightedMangaTitle.set(ch.mangaTitle);
          }
        });
      }
    }
  }

  onEnterManga(titleVal: string): void {
    const title = titleVal.trim();
    this.highlightedMangaTitle.set(title || null);
    this.refreshTrigger.update((v) => v + 1);
  }

  onOpenChapter(ch: Chapter): void {
    localStorage.setItem('manga:lastChapterId', String(ch.id));
    this.router.navigate(['/chapters', ch.id]);
  }
}


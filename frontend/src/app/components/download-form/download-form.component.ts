import { Component, signal, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { Chapter } from '../../models/chapter.model';

@Component({
  selector: 'app-download-form',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './download-form.component.html',
  styleUrl: './download-form.component.scss'
})
export class DownloadFormComponent {
  chapterUrl = '';
  loading = signal(false);
  error = signal<string | null>(null);
  progress = signal<{ current: number; total: number } | null>(null);
  downloaded = output<Chapter>();
  downloadError = output<string>();

  constructor(private api: ApiService) {}

  onSubmit(): void {
    const url = this.chapterUrl.trim();
    if (!url) {
      this.error.set('Enter a chapter page URL.');
      return;
    }
    const supported = ['mangadex.org', 'globalcomix.com', 'mangaplus.shueisha.co.jp'];
    if (!supported.some(d => url.includes(d))) {
      this.error.set('Use a chapter URL from MangaDex, GlobalComix, or MangaPlus.');
      return;
    }
    this.error.set(null);
    this.loading.set(true);
    this.progress.set({ current: 0, total: 1 });
    this.api.startDownload(url).subscribe({
      next: (res) => {
        this.loading.set(false);
        this.progress.set(null);
        if (res && 'id' in res) {
          const ch = res as Chapter;
          this.downloaded.emit(ch);
          this.chapterUrl = '';
        } else if (res && 'message' in res) {
          const msg = (res as { message: string }).message;
          this.error.set(msg);
          this.downloadError.emit(msg);
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.progress.set(null);
        const msg = err.error?.message || err.message || 'Download failed';
        this.error.set(msg);
        this.downloadError.emit(msg);
      }
    });
  }
}

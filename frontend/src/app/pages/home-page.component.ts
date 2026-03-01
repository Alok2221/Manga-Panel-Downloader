import { Component, signal, OnDestroy } from '@angular/core';
import { DatePipe } from '@angular/common';
import { DownloadFormComponent } from '../components/download-form/download-form.component';
import { ChapterListComponent } from '../components/chapter-list/chapter-list.component';
import { Chapter } from '../models/chapter.model';

export type StatusEntry = { text: string; type: 'success' | 'error' | 'info'; at: Date };

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [DatePipe, DownloadFormComponent, ChapterListComponent],
  templateUrl: './home-page.component.html',
  styleUrl: './home-page.component.scss'
})
export class HomePageComponent implements OnDestroy {
  refreshListTrigger = signal(0);
  searchTitle = signal('');
  searchChapterNum = signal<number | undefined>(undefined);
  statusLog = signal<StatusEntry[]>([]);
  private refreshIntervalId: ReturnType<typeof setInterval> | null = null;

  onDownloaded(ch: Chapter): void {
    const total = ch.totalPanels ?? 0;
    const msg = total > 0
      ? `Chapter added. Downloading ${total} panels in the background. The list below will update every few seconds.`
      : 'Chapter added. Panels are downloading in the background.';
    this.addStatus(msg, 'success');
    this.refreshListTrigger.update((v) => v + 1);
    this.startProgressPolling();
  }

  onDownloadError(msg: string): void {
    this.addStatus(msg, 'error');
  }

  private addStatus(text: string, type: StatusEntry['type']): void {
    this.statusLog.update(log => [{ text, type, at: new Date() }, ...log.slice(0, 9)]);
  }

  private startProgressPolling(): void {
    this.stopProgressPolling();
    this.refreshIntervalId = setInterval(() => {
      this.refreshListTrigger.update(v => v + 1);
    }, 4000);
    setTimeout(() => this.stopProgressPolling(), 120000);
  }

  private stopProgressPolling(): void {
    if (this.refreshIntervalId != null) {
      clearInterval(this.refreshIntervalId);
      this.refreshIntervalId = null;
    }
  }

  ngOnDestroy(): void {
    this.stopProgressPolling();
  }

  onSearchChange(title: string, chapterVal: string): void {
    this.searchTitle.set(title.trim());
    const n = parseFloat(chapterVal);
    this.searchChapterNum.set(isNaN(n) ? undefined : n);
  }

  dismissStatus(index: number): void {
    this.statusLog.update(log => log.filter((_, i) => i !== index));
  }
}

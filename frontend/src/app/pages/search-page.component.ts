import { Component, signal } from '@angular/core';
import { ChapterListComponent } from '../components/chapter-list/chapter-list.component';

@Component({
  selector: 'app-search-page',
  standalone: true,
  imports: [ChapterListComponent],
  templateUrl: './search-page.component.html',
  styleUrl: './search-page.component.scss'
})
export class SearchPageComponent {
  searchTitle = signal('');
  searchChapterNum = signal<number | undefined>(undefined);
  searchVolume = signal<string | null>(null);
  filtersOpen = signal(false);
  refreshListTrigger = signal(0);

  onTitleChange(value: string): void {
    this.searchTitle.set(value.trim());
    this.refreshListTrigger.update((v) => v + 1);
  }

  onChapterChange(value: string): void {
    const s = value.trim();
    if (s === '') {
      this.searchChapterNum.set(undefined);
    } else {
      const n = parseFloat(s);
      this.searchChapterNum.set(isNaN(n) ? undefined : n);
    }
    this.refreshListTrigger.update((v) => v + 1);
  }

  onVolumeChange(value: string): void {
    const vol = value.trim();
    this.searchVolume.set(vol || null);
    this.refreshListTrigger.update((v) => v + 1);
  }

  toggleFilters(): void {
    this.filtersOpen.update((v) => !v);
  }
}


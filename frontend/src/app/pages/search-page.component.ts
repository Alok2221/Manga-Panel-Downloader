import { Component, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ChapterListComponent } from '../components/chapter-list/chapter-list.component';

@Component({
  selector: 'app-search-page',
  standalone: true,
  imports: [DatePipe, ChapterListComponent],
  templateUrl: './search-page.component.html',
  styleUrl: './search-page.component.scss'
})
export class SearchPageComponent {
  searchTitle = signal('');
  searchChapterNum = signal<number | undefined>(undefined);
  refreshListTrigger = signal(0);

  onSearchChange(title: string, chapterVal: string): void {
    this.searchTitle.set(title.trim());
    const n = parseFloat(chapterVal);
    this.searchChapterNum.set(isNaN(n) ? undefined : n);
    this.refreshListTrigger.update((v) => v + 1);
  }
}


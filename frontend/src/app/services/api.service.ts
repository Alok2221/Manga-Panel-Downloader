import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Chapter, ChapterGrouped, ChapterPage } from '../models/chapter.model';
import { Panel } from '../models/panel.model';
import { PanelTextSegment } from '../models/panel-text-segment.model';

export interface DownloadResponse {
  id?: number;
  message?: string;
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private baseUrl = '/api';

  constructor(private http: HttpClient) {}

  startDownload(chapterUrl: string): Observable<Chapter | DownloadResponse> {
    return this.http.post<Chapter | DownloadResponse>(`${this.baseUrl}/download`, { chapterUrl });
  }

  getChapters(page = 0, size = 20, title?: string, chapter?: number): Observable<ChapterPage> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (title) params = params.set('title', title);
    if (chapter != null) params = params.set('chapter', chapter);
    return this.http.get<ChapterPage>(`${this.baseUrl}/chapters`, { params });
  }

  getChapter(id: number): Observable<Chapter> {
    return this.http.get<Chapter>(`${this.baseUrl}/chapters/${id}`);
  }

  getPanels(chapterId: number): Observable<Panel[]> {
    return this.http.get<Panel[]>(`${this.baseUrl}/chapters/${chapterId}/panels`);
  }

  getPanelImageUrl(panelId: number): string {
    return `${this.baseUrl}/panels/${panelId}/image`;
  }

  deleteChapter(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/chapters/${id}`);
  }

  /** Reindex chapter IDs to 1, 2, 3, … (call after deletions). */
  reindexChapters(): Observable<{ status: string; message: string }> {
    return this.http.post<{ status: string; message: string }>(`${this.baseUrl}/chapters/reindex`, {});
  }

  search(title?: string, chapter?: number, volume?: string, page = 0, size = 20): Observable<ChapterPage> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (title) params = params.set('title', title);
    if (chapter != null) params = params.set('chapter', chapter);
    if (volume != null && volume !== '') params = params.set('volume', volume);
    return this.http.get<ChapterPage>(`${this.baseUrl}/search`, { params });
  }

  /** Chapters grouped by manga and volume (for Read page). */
  getChaptersGrouped(title?: string | null, chapter?: number | null, volume?: string | null): Observable<ChapterGrouped[]> {
    let params = new HttpParams();
    if (title != null && title !== '') params = params.set('title', title);
    if (chapter != null) params = params.set('chapter', String(chapter));
    if (volume != null && volume !== '') params = params.set('volume', volume);
    return this.http.get<ChapterGrouped[]>(`${this.baseUrl}/chapters/grouped`, { params });
  }

  /** Chapter sequence for navigating between chapters when reading whole manga. */
  getChapterSequence(mangaTitle: string): Observable<Chapter[]> {
    const params = new HttpParams().set('mangaTitle', mangaTitle);
    return this.http.get<Chapter[]>(`${this.baseUrl}/chapters/sequence`, { params });
  }

  /** Get text segments (OCR/translation) for a chapter. */
  getChapterTexts(chapterId: number): Observable<PanelTextSegment[]> {
    return this.http.get<PanelTextSegment[]>(`${this.baseUrl}/chapters/${chapterId}/texts`);
  }

  /** Trigger OCR to extract text from chapter panels. */
  startChapterOcr(chapterId: number, sourceLanguage?: string): Observable<{ status: string }> {
    let params = new HttpParams();
    if (sourceLanguage) params = params.set('sourceLanguage', sourceLanguage);
    return this.http.post<{ status: string }>(`${this.baseUrl}/chapters/${chapterId}/ocr`, {}, { params });
  }

  /** Trigger translation of chapter text segments (EN→PL). */
  startChapterTranslation(chapterId: number, targetLanguage?: string): Observable<{ status: string }> {
    let params = new HttpParams();
    if (targetLanguage) params = params.set('targetLanguage', targetLanguage);
    return this.http.post<{ status: string }>(`${this.baseUrl}/chapters/${chapterId}/translate`, {}, { params });
  }
}

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Chapter, ChapterPage } from '../models/chapter.model';
import { Panel } from '../models/panel.model';

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

  search(title?: string, chapter?: number, page = 0, size = 20): Observable<ChapterPage> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (title) params = params.set('title', title);
    if (chapter != null) params = params.set('chapter', chapter);
    return this.http.get<ChapterPage>(`${this.baseUrl}/search`, { params });
  }
}

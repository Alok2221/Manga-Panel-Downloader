export interface Chapter {
  id: number;
  mangaId: number | null;
  mangaTitle: string | null;
  chapterNumber: number | null;
  title: string | null;
  url: string;
  totalPanels: number;
  panelsDownloaded?: number;
  downloadedAt: string;
  language: string | null;
}

export interface ChapterPage {
  content: Chapter[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

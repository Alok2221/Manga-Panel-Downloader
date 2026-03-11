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
  volume?: string | null;
   scanlationGroup?: string | null;
}

export interface VolumeGroup {
  volume: string;
  chapters: Chapter[];
}

export interface ChapterGrouped {
  mangaId: number | null;
  mangaTitle: string;
  mangaCoverUrl?: string | null;
  volumes: VolumeGroup[];
}

export interface ChapterPage {
  content: Chapter[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

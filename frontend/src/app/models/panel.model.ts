export interface Panel {
  id: number;
  chapterId: number;
  pageNumber: number;
  imageUrl: string;
  localPath: string | null;
  width: number | null;
  height: number | null;
  fileSize: number | null;
  format: string | null;
}

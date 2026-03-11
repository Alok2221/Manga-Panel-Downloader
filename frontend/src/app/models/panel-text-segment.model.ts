export interface PanelTextSegment {
  id: number;
  panelId: number;
  sequenceIndex: number;
  sourceLanguage: string | null;
  targetLanguage: string | null;
  sourceText: string;
  translatedText: string | null;
  bboxX: number | null;
  bboxY: number | null;
  bboxW: number | null;
  bboxH: number | null;
}

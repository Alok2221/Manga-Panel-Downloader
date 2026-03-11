import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';
import { PanelGalleryComponent } from './panel-gallery.component';
import { ApiService } from '../../services/api.service';
import { Chapter } from '../../models/chapter.model';
import { Panel } from '../../models/panel.model';
import { PanelTextSegment } from '../../models/panel-text-segment.model';

describe('PanelGalleryComponent', () => {
  let component: PanelGalleryComponent;
  let fixture: ComponentFixture<PanelGalleryComponent>;
  let apiSpy: jasmine.SpyObj<ApiService>;

  const mockChapter: Chapter = {
    id: 1,
    mangaTitle: 'Test Manga',
    chapterNumber: 1,
    title: 'Ch1',
    url: 'https://example.com/ch1',
    totalPanels: 2,
    panelsDownloaded: 2,
    downloadedAt: new Date().toISOString(),
    language: 'en',
    volume: null,
    scanlationGroup: null,
    mangaId: 10
  };

  const mockPanels: Panel[] = [
    { id: 100, chapterId: 1, pageNumber: 1, imageUrl: '', localPath: null, width: null, height: null, fileSize: null, format: null },
    { id: 101, chapterId: 1, pageNumber: 2, imageUrl: '', localPath: null, width: null, height: null, fileSize: null, format: null }
  ];

  const mockSegments: PanelTextSegment[] = [
    { id: 1, panelId: 100, sequenceIndex: 0, sourceLanguage: 'en', targetLanguage: 'pl', sourceText: 'Hi', translatedText: 'Cześć', bboxX: null, bboxY: null, bboxW: null, bboxH: null }
  ];

  beforeEach(async () => {
    apiSpy = jasmine.createSpyObj('ApiService', ['getChapter', 'getPanels', 'getChapterTexts', 'startChapterOcr', 'startChapterTranslation', 'getPanelImageUrl', 'getChapterSequence']);
    apiSpy.getChapter.and.returnValue(of(mockChapter));
    apiSpy.getPanels.and.returnValue(of(mockPanels));
    apiSpy.getChapterTexts.and.returnValue(of(mockSegments));
    apiSpy.startChapterTranslation.and.returnValue(of({ status: 'completed' }));
    apiSpy.getPanelImageUrl.and.callFake((id: number) => `/api/panels/${id}/image`);
    apiSpy.getChapterSequence.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [PanelGalleryComponent, HttpClientTestingModule],
      providers: [
        { provide: ApiService, useValue: apiSpy },
        { provide: ActivatedRoute, useValue: { paramMap: of(new Map([['id', '1']])), queryParamMap: of(new Map()) } },
        { provide: Router, useValue: { navigate: jasmine.createSpy('navigate') } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PanelGalleryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call startChapterTranslation when startTranslation is invoked', () => {
    component.chapter.set(mockChapter);
    component.segments.set(mockSegments);

    component.startTranslation();

    expect(apiSpy.startChapterTranslation).toHaveBeenCalledWith(1);
  });

  it('after translation completes should refresh chapter texts', () => {
    component.chapter.set(mockChapter);
    component.segments.set(mockSegments);
    apiSpy.getChapterTexts.and.returnValue(of([...mockSegments, { ...mockSegments[0], id: 2, translatedText: 'Hej' }]));

    component.startTranslation();

    expect(apiSpy.startChapterTranslation).toHaveBeenCalledWith(1);
    expect(apiSpy.getChapterTexts).toHaveBeenCalledWith(1);
  });
});

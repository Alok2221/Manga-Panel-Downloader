import { Component, signal, OnInit } from '@angular/core';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { Chapter } from '../../models/chapter.model';
import { Panel } from '../../models/panel.model';

@Component({
  selector: 'app-panel-gallery',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './panel-gallery.component.html',
  styleUrl: './panel-gallery.component.scss'
})
export class PanelGalleryComponent implements OnInit {
  chapter = signal<Chapter | null>(null);
  panels = signal<Panel[]>([]);
  loading = signal(true);

  constructor(
    private api: ApiService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.loading.set(false);
      return;
    }
    this.api.getChapter(id).subscribe({
      next: (c) => this.chapter.set(c),
      error: () => this.loading.set(false)
    });
    this.api.getPanels(id).subscribe({
      next: (p) => {
        this.panels.set(p);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  panelImageUrl(panelId: number): string {
    return this.api.getPanelImageUrl(panelId);
  }

  downloadPanel(panel: Panel, event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    const url = this.api.getPanelImageUrl(panel.id);
    const ext = panel.format || 'png';
    const name = `panel-${panel.pageNumber}.${ext}`;
    fetch(url)
      .then((r) => r.blob())
      .then((blob) => {
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = name;
        a.click();
        URL.revokeObjectURL(a.href);
      });
  }
}

import { Routes } from '@angular/router';
import { HomePageComponent } from './pages/home-page.component';
import { PanelGalleryComponent } from './components/panel-gallery/panel-gallery.component';
import { ReadMangaPageComponent } from './pages/read-manga-page.component';
import { SearchPageComponent } from './pages/search-page.component';

export const routes: Routes = [
  { path: '', component: HomePageComponent },
  { path: 'search', component: SearchPageComponent },
  { path: 'read', component: ReadMangaPageComponent },
  { path: 'chapters/:id', component: PanelGalleryComponent },
  { path: '**', redirectTo: '' }
];

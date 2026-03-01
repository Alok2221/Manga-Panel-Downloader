import { Routes } from '@angular/router';
import { HomePageComponent } from './pages/home-page.component';
import { PanelGalleryComponent } from './components/panel-gallery/panel-gallery.component';

export const routes: Routes = [
  { path: '', component: HomePageComponent },
  { path: 'chapters/:id', component: PanelGalleryComponent },
  { path: '**', redirectTo: '' }
];

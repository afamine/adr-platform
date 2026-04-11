import { Routes } from '@angular/router';

import { AdrEditorPageComponent } from './pages/adr-editor-page/adr-editor-page.component';
import { LoginComponent } from './pages/auth/login/login.component';
import { RegisterComponent } from './pages/register/register.component';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'adrs', component: AdrEditorPageComponent, canActivate: [authGuard] },
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  {
    path: '**',
    redirectTo: '/login'
  }
];

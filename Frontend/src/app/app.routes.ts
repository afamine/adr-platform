import { Routes } from '@angular/router';

import { AdrEditorPageComponent } from './pages/adr-editor-page/adr-editor-page.component';
import { ForgotPasswordComponent } from './pages/auth/forgot-password/forgot-password.component';
import { LoginComponent } from './pages/auth/login/login.component';
import { RegisterComponent } from './pages/register/register.component';
import { UnauthorizedComponent } from './pages/unauthorized/unauthorized.component';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'adrs', component: AdrEditorPageComponent, canActivate: [authGuard] },
  { path: 'unauthorized', component: UnauthorizedComponent },
  {
    path: '**',
    redirectTo: '/login'
  }
];

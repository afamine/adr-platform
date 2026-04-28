import { Routes } from '@angular/router';

import { AdrEditorPageComponent } from './pages/adr-editor-page/adr-editor-page.component';
import { ForgotPasswordComponent } from './pages/auth/forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './pages/auth/reset-password/reset-password.component';
import { VerifyEmailComponent } from './pages/auth/verify-email/verify-email.component';
import { VerifyEmailPendingComponent } from './pages/auth/verify-email-pending/verify-email-pending.component';
import { VerifyEmailSentComponent } from './pages/auth/verify-email-sent/verify-email-sent.component';
import { LoginComponent } from './pages/auth/login/login.component';
import { RegisterComponent } from './pages/register/register.component';
import { UnauthorizedComponent } from './pages/unauthorized/unauthorized.component';
import { AdminPlaceholderComponent } from './pages/admin/admin-placeholder.component';
import { authGuard } from './guards/auth.guard';
import { adminGuard } from './guards/admin.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'reset-password', component: ResetPasswordComponent },
  { path: 'verify-email', component: VerifyEmailComponent },
  { path: 'verify-email-pending', component: VerifyEmailPendingComponent },
  { path: 'verify-email-sent', component: VerifyEmailSentComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'adrs', component: AdrEditorPageComponent, canActivate: [authGuard] },
  {
    path: 'admin',
    canActivate: [adminGuard],
    children: [
      { path: 'users', component: AdminPlaceholderComponent },
      { path: '', redirectTo: 'users', pathMatch: 'full' }
    ]
  },
  { path: 'unauthorized', component: UnauthorizedComponent },
  {
    path: '**',
    redirectTo: '/login'
  }
];

import { Routes } from '@angular/router';

import { AdrDashboardComponent } from './pages/adrs/adr-dashboard.component';
import { ForgotPasswordComponent } from './pages/auth/forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './pages/auth/reset-password/reset-password.component';
import { VerifyEmailComponent } from './pages/auth/verify-email/verify-email.component';
import { VerifyEmailPendingComponent } from './pages/auth/verify-email-pending/verify-email-pending.component';
import { VerifyEmailSentComponent } from './pages/auth/verify-email-sent/verify-email-sent.component';
import { LoginComponent } from './pages/auth/login/login.component';
import { RegisterComponent } from './pages/register/register.component';
import { AcceptInviteComponent } from './pages/auth/accept-invite/accept-invite.component';
import { ChangePasswordComponent } from './pages/auth/change-password/change-password.component';
import { UnauthorizedComponent } from './pages/unauthorized/unauthorized.component';
import { UserManagementComponent } from './pages/admin/user-management/user-management.component';
import { WorkspaceSettingsComponent } from './pages/admin/workspace-settings/workspace-settings.component';
import { AnalyticsDashboardComponent } from './pages/admin/analytics-dashboard/analytics-dashboard.component';
import { UserProfileComponent } from './pages/profile/user-profile.component';
import { NotificationsComponent } from './pages/notifications/notifications.component';
import { authGuard } from './guards/auth.guard';
import { adminGuard } from './guards/admin.guard';
import { analyticsGuard } from './guards/analytics.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'reset-password', component: ResetPasswordComponent },
  { path: 'verify-email', component: VerifyEmailComponent },
  { path: 'verify-email-pending', component: VerifyEmailPendingComponent },
  { path: 'verify-email-sent', component: VerifyEmailSentComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'accept-invite', component: AcceptInviteComponent },
  { path: 'change-password', component: ChangePasswordComponent, canActivate: [authGuard] },
  { path: 'adrs', component: AdrDashboardComponent, canActivate: [authGuard] },
  { path: 'profile', component: UserProfileComponent, canActivate: [authGuard] },
  { path: 'notifications', component: NotificationsComponent, canActivate: [authGuard] },
  {
    path: 'admin',
    canActivate: [adminGuard],
    children: [
      { path: 'users', component: UserManagementComponent },
      { path: 'settings', component: WorkspaceSettingsComponent },
      { path: 'analytics', component: AnalyticsDashboardComponent, canActivate: [analyticsGuard] },
      { path: '', redirectTo: 'users', pathMatch: 'full' }
    ]
  },
  { path: 'unauthorized', component: UnauthorizedComponent },
  {
    path: '**',
    redirectTo: '/login'
  }
];

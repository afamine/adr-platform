import { Routes } from '@angular/router';

import { LoginComponent } from './pages/auth/login/login.component';
import { RegisterComponent } from './pages/register/register.component';
import { authGuard } from './guards/auth.guard';
import { adminGuard } from './guards/admin.guard';
import { analyticsGuard } from './guards/analytics.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent, title: 'Sign in - Axiom ADR' },
  {
    path: 'forgot-password',
    title: 'Forgot Password - Axiom ADR',
    loadComponent: () => import('./pages/auth/forgot-password/forgot-password.component').then((m) => m.ForgotPasswordComponent)
  },
  {
    path: 'reset-password',
    title: 'Reset Password - Axiom ADR',
    loadComponent: () => import('./pages/auth/reset-password/reset-password.component').then((m) => m.ResetPasswordComponent)
  },
  {
    path: 'verify-email',
    title: 'Verify Email - Axiom ADR',
    loadComponent: () => import('./pages/auth/verify-email/verify-email.component').then((m) => m.VerifyEmailComponent)
  },
  {
    path: 'verify-email-pending',
    title: 'Verify Email - Axiom ADR',
    loadComponent: () => import('./pages/auth/verify-email-pending/verify-email-pending.component').then((m) => m.VerifyEmailPendingComponent)
  },
  {
    path: 'verify-email-sent',
    title: 'Verify Email - Axiom ADR',
    loadComponent: () => import('./pages/auth/verify-email-sent/verify-email-sent.component').then((m) => m.VerifyEmailSentComponent)
  },
  {
    path: '2fa-verify',
    title: 'Two-Factor Verification - Axiom ADR',
    loadComponent: () =>
      import('./pages/auth/totp-verify/totp-verify.component')
        .then(m => m.TotpVerifyComponent)
  },
  {
    path: 'setup-2fa',
    title: 'Set Up 2FA - Axiom ADR',
    loadComponent: () =>
      import('./pages/auth/setup-2fa/setup-2fa.component')
        .then(m => m.Setup2faComponent),
    canActivate: [authGuard]
  },
  { path: 'register', component: RegisterComponent, title: 'Create Account - Axiom ADR' },
  {
    path: 'accept-invite',
    title: 'Accept Invitation - Axiom ADR',
    loadComponent: () => import('./pages/auth/accept-invite/accept-invite.component').then((m) => m.AcceptInviteComponent)
  },
  {
    path: 'change-password',
    title: 'Change Password - Axiom ADR',
    loadComponent: () => import('./pages/auth/change-password/change-password.component').then((m) => m.ChangePasswordComponent),
    canActivate: [authGuard]
  },
  {
    path: 'adrs',
    title: 'Dashboard - Axiom ADR',
    loadComponent: () => import('./pages/adrs/adr-dashboard.component').then((m) => m.AdrDashboardComponent),
    canActivate: [authGuard]
  },
  {
    path: 'profile',
    title: 'Profile - Axiom ADR',
    loadComponent: () => import('./pages/profile/user-profile.component').then((m) => m.UserProfileComponent),
    canActivate: [authGuard]
  },
  {
    path: 'notifications',
    title: 'Notifications - Axiom ADR',
    loadComponent: () => import('./pages/notifications/notifications.component').then((m) => m.NotificationsComponent),
    canActivate: [authGuard]
  },
  {
    path: 'admin',
    title: 'Admin - Axiom ADR',
    canActivate: [adminGuard],
    children: [
      {
        path: 'dashboard',
        redirectTo: 'users',
        pathMatch: 'full'
      },
      {
        path: 'users',
        title: 'Admin - Axiom ADR',
        loadComponent: () => import('./pages/admin/user-management/user-management.component').then((m) => m.UserManagementComponent)
      },
      {
        path: 'settings',
        title: 'Admin - Axiom ADR',
        loadComponent: () => import('./pages/admin/workspace-settings/workspace-settings.component').then((m) => m.WorkspaceSettingsComponent)
      },
      {
        path: 'analytics',
        title: 'Analytics - Axiom ADR',
        loadComponent: () => import('./pages/admin/analytics-dashboard/analytics-dashboard.component').then((m) => m.AnalyticsDashboardComponent),
        canActivate: [analyticsGuard]
      },
      {
        path: 'audit-log',
        title: 'Admin - Axiom ADR',
        loadComponent: () =>
          import('./pages/admin/audit-log/audit-log.component')
            .then((m) => m.AuditLogComponent)
      },
      { path: '', redirectTo: 'users', pathMatch: 'full' }
    ]
  },
  {
    path: 'unauthorized',
    title: 'Unauthorized - Axiom ADR',
    loadComponent: () => import('./pages/unauthorized/unauthorized.component').then((m) => m.UnauthorizedComponent)
  },
  {
    path: '**',
    redirectTo: '/login'
  }
];

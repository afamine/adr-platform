import { CommonModule, Location } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';

import { AuthUser, NotificationPreferences, Role } from '../../models/auth.models';
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';

interface RoleConfig {
  bg: string;
  color: string;
  label: string;
  accessLabel: string;
  permissions: string[];
}

@Component({
  selector: 'app-user-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './user-profile.component.html',
  styleUrls: ['./user-profile.component.scss']
})
export class UserProfileComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly location = inject(Location);

  currentUser: AuthUser | null = this.authService.getCurrentUser();

  emailNotifications = true;
  voteNotifications = true;
  statusNotifications = true;
  slackNotifications = false;

  isLoading = false;
  isSaving = false;

  private readonly roleConfigs: Record<Role, RoleConfig> = {
    [Role.ADMIN]: {
      bg: '#faece7',
      color: '#993c1d',
      label: 'ADMIN',
      accessLabel: 'Full access',
      permissions: [
        'Create and manage all ADRs',
        'Assign roles to team members',
        'Configure workspace settings',
        'Final vote approval rights'
      ]
    },
    [Role.APPROVER]: {
      bg: '#eeedfe',
      color: '#3c3489',
      label: 'APPROVER',
      accessLabel: 'Approval access',
      permissions: [
        'Cast final vote (ACCEPT/REJECT)',
        'Vote on ADRs under review',
        'Access Analytics Dashboard'
      ]
    },
    [Role.REVIEWER]: {
      bg: '#faeeda',
      color: '#854f0b',
      label: 'REVIEWER',
      accessLabel: 'Review access',
      permissions: [
        'Move ADRs to Under Review',
        'Vote APPROVE or REJECT',
        'Comment on ADRs'
      ]
    },
    [Role.AUTHOR]: {
      bg: '#e1f5ee',
      color: '#0f6e56',
      label: 'AUTHOR',
      accessLabel: 'Author access',
      permissions: [
        'Create and submit ADRs',
        'Edit own ADRs (DRAFT/PROPOSED)',
        'Mark ADRs as SUPERSEDED'
      ]
    }
  };

  get roleConfig(): RoleConfig {
    const role = this.currentUser?.role ?? Role.AUTHOR;
    return this.roleConfigs[role] ?? this.roleConfigs[Role.AUTHOR];
  }

  get userInitials(): string {
    return (this.currentUser?.fullName || '??')
      .split(' ')
      .filter(Boolean)
      .map((n) => n[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  }

  get memberSince(): string {
    if (!this.currentUser?.createdAt) return '—';
    const d = new Date(this.currentUser.createdAt);
    if (isNaN(d.getTime())) return '—';
    return new Intl.DateTimeFormat('en-US', { month: 'long', day: 'numeric', year: 'numeric' }).format(d);
  }

  ngOnInit(): void {
    this.loadProfile();
    this.loadPreferences();
  }

  loadProfile(): void {
    this.authService.getMyProfile().subscribe({
      next: (user) => {
        this.currentUser = user;
        localStorage.setItem('adr_user', JSON.stringify(user));
      },
      error: (err) => {
        this.notificationService.error('Failed to load profile', this.getErrorMessage(err));
      }
    });
  }

  loadPreferences(): void {
    this.authService.getMyPreferences().subscribe({
      next: (prefs: NotificationPreferences) => {
        this.emailNotifications = prefs.emailOnReview;
        this.voteNotifications = prefs.emailOnVote;
        this.statusNotifications = prefs.emailOnStatus;
        this.slackNotifications = prefs.slackEnabled;
      },
      error: (err) => {
        this.notificationService.error('Failed to load preferences', this.getErrorMessage(err));
      }
    });
  }

  savePreferences(): void {
    this.isSaving = true;
    const payload: NotificationPreferences = {
      emailOnReview: this.emailNotifications,
      emailOnVote: this.voteNotifications,
      emailOnStatus: this.statusNotifications,
      slackEnabled: this.slackNotifications
    };
    this.authService.updateMyPreferences(payload).subscribe({
      next: () => {
        this.isSaving = false;
        this.notificationService.success('Preferences saved', 'Your notification settings have been updated.');
      },
      error: (err) => {
        this.isSaving = false;
        this.notificationService.error('Failed to save preferences', this.getErrorMessage(err));
      }
    });
  }

  signOutAllDevices(): void {
    this.authService.logoutAllDevices().subscribe({
      next: () => {
        this.notificationService.success('Signed out', 'All devices have been signed out.');
        this.authService.logout();
      },
      error: () => {
        this.authService.logout();
      }
    });
  }

  goBack(): void {
    this.location.back();
  }

  goToChangePassword(): void {
    void this.router.navigate(['/change-password']);
  }

  private getErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const body = error.error as { message?: string } | null;
      return body?.message || error.message || 'Please try again.';
    }
    return 'Please try again.';
  }
}

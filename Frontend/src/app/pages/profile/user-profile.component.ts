import { CommonModule, Location } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';

import { AuthUser, NotificationPreferences, Role } from '../../models/auth.models';
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';
import { ChangePasswordModalComponent } from '../../components/change-password-modal/change-password-modal.component';

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
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule, ChangePasswordModalComponent],
  templateUrl: './user-profile.component.html',
  styleUrls: ['./user-profile.component.scss']
})
export class UserProfileComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly location = inject(Location);
  private readonly fb = inject(FormBuilder);

  currentUser: AuthUser | null = this.authService.getCurrentUser();

  isEditing = false;
  editForm: FormGroup = this.fb.group({
    fullName: ['', [Validators.required, Validators.maxLength(100)]]
  });

  showColorPicker = false;
  readonly avatarColors = [
    '#0F172A', '#6366F1', '#1D9E75', '#DC2626', '#D97706',
    '#7C3AED', '#DB2777', '#0891B2', '#16A34A', '#EA580C'
  ];
  selectedColor = this.currentUser?.avatarColor || '#0F172A';

  emailNotifications = true;
  voteNotifications = true;
  statusNotifications = true;
  slackNotifications = false;

  isLoading = false;
  isSaving = false;
  showChangePasswordModal = false;

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

  startEditing(): void {
    this.isEditing = true;
    this.editForm.patchValue({ fullName: this.currentUser?.fullName || '' });
  }

  cancelEditing(): void {
    this.isEditing = false;
  }

  saveProfile(): void {
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }
    this.isSaving = true;
    const { fullName } = this.editForm.value as { fullName: string };
    this.authService.updateMyProfile({ fullName }).subscribe({
      next: (updated) => {
        this.isSaving = false;
        this.isEditing = false;
        this.currentUser = updated;
        localStorage.setItem('adr_user', JSON.stringify(updated));
        this.notificationService.success('Profile updated', 'Your name has been saved.');
      },
      error: (err) => {
        this.isSaving = false;
        this.notificationService.error('Update failed', this.getErrorMessage(err));
      }
    });
  }

  openColorPicker(): void {
    this.showColorPicker = true;
  }

  selectAvatarColor(color: string): void {
    this.selectedColor = color;
    this.authService.updateMyProfile({
      fullName: this.currentUser?.fullName || '',
      avatarColor: color
    }).subscribe({
      next: (updated) => {
        this.currentUser = updated;
        this.showColorPicker = false;
        localStorage.setItem('adr_user', JSON.stringify(updated));
        this.notificationService.success('Avatar updated', 'Your avatar color has been changed.');
      },
      error: () => {
        this.showColorPicker = false;
        this.notificationService.error('Update failed', 'Could not save avatar color.');
      }
    });
  }

  loadProfile(): void {
    this.authService.getMyProfile().subscribe({
      next: (user) => {
        this.currentUser = user;
        this.selectedColor = user.avatarColor || '#0F172A';
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
    this.showChangePasswordModal = true;
  }

  onCloseChangePassword(): void {
    this.showChangePasswordModal = false;
  }

  private getErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const body = error.error as { message?: string } | null;
      return body?.message || error.message || 'Please try again.';
    }
    return 'Please try again.';
  }
}

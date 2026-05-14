import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, HostListener, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthUser, Role } from '../../../models/auth.models';
import { AuthService } from '../../../services/auth.service';
import { NotificationService } from '../../../services/notification.service';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.scss']
})
export class UserManagementComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);

  users: AuthUser[] = [];
  filteredUsers: AuthUser[] = [];
  searchQuery = '';
  roleFilter: Role | 'ALL' = 'ALL';
  openDropdownId: string | null = null;
  currentUser = this.authService.getCurrentUser();
  isLoading = false;
  showInviteModal = false;
  inviteEmail = '';
  inviteRole: Role = Role.REVIEWER;
  isInviting = false;

  readonly roles: Role[] = [Role.AUTHOR, Role.REVIEWER, Role.APPROVER, Role.ADMIN];
  readonly roleConfig: Record<Role, { bg: string; color: string }> = {
    [Role.ADMIN]: { bg: '#faece7', color: '#993c1d' },
    [Role.APPROVER]: { bg: '#eeedfe', color: '#3c3489' },
    [Role.REVIEWER]: { bg: '#faeeda', color: '#854f0b' },
    [Role.AUTHOR]: { bg: '#e1f5ee', color: '#0f6e56' }
  };
  readonly avatarColors: Record<Role, string> = {
    [Role.ADMIN]: '#0f172a',
    [Role.APPROVER]: '#1d9e75',
    [Role.REVIEWER]: '#6366f1',
    [Role.AUTHOR]: '#378add'
  };

  ngOnInit(): void {
    this.loadUsers();
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement | null;
    if (!target?.closest('.actions-cell')) {
      this.openDropdownId = null;
    }
  }

  loadUsers(): void {
    this.isLoading = true;
    this.authService.getUsersInWorkspace().subscribe({
      next: (users) => {
        this.users = users;
        this.applyFilter();
        this.isLoading = false;
      },
      error: (error) => {
        this.isLoading = false;
        this.notificationService.error('Unable to load users', this.getErrorMessage(error));
      }
    });
  }

  applyFilter(): void {
    const query = this.searchQuery.trim().toLowerCase();
    this.filteredUsers = this.users.filter((user: AuthUser) => {
      const fullName = user.fullName?.toLowerCase() ?? '';
      const email = user.email?.toLowerCase() ?? '';
      const matchesSearch = !query || fullName.includes(query) || email.includes(query);
      const matchesRole = this.roleFilter === 'ALL' || user.role === this.roleFilter;
      return matchesSearch && matchesRole;
    });
  }

  onSearchChange(query: string): void {
    this.searchQuery = query;
    this.applyFilter();
  }

  onRoleFilterChange(role: Role | 'ALL'): void {
    this.roleFilter = role;
    this.applyFilter();
  }

  toggleDropdown(id: string): void {
    this.openDropdownId = this.openDropdownId === id ? null : id;
  }

  changeRole(userId: string, newRole: Role): void {
    const existingUser = this.users.find((u) => u.id === userId);
    if (this.isCurrentUser(userId) || existingUser?.role === newRole) {
      this.openDropdownId = null;
      return;
    }

    this.openDropdownId = null;
    this.authService.updateUserRole(userId, newRole).subscribe({
      next: () => {
        this.notificationService.success(
          'Role updated',
          `${existingUser?.fullName ?? 'User'} is now ${newRole.toLowerCase()}.`
        );
        this.loadUsers();
      },
      error: (error) => {
        this.notificationService.error('Unable to update role', this.getErrorMessage(error));
      }
    });
  }

  openInviteModal(): void {
    this.inviteEmail = '';
    this.inviteRole = Role.REVIEWER;
    this.showInviteModal = true;
  }

  closeInviteModal(): void {
    if (this.isInviting) return;
    this.showInviteModal = false;
  }

  submitInvite(): void {
    const email = this.inviteEmail.trim().toLowerCase();
    if (!email) return;

    this.isInviting = true;
    this.authService.inviteUser(email, this.inviteRole).subscribe({
      next: () => {
        this.isInviting = false;
        this.showInviteModal = false;
        this.notificationService.success(
          'Invitation sent',
          `An invitation has been sent to ${email}.`
        );
        this.loadUsers();
      },
      error: (error) => {
        this.isInviting = false;
        this.notificationService.error('Invitation failed', this.getErrorMessage(error));
      }
    });
  }

  deactivateUser(user: AuthUser): void {
    this.openDropdownId = null;
    const newStatus = !user.isActive;
    this.authService.updateUserStatus(user.id, newStatus).subscribe({
      next: () => {
        const label = newStatus ? 'activated' : 'deactivated';
        this.notificationService.success(
          `User ${label}`,
          `${user.fullName} has been ${label}.`
        );
        this.loadUsers();
      },
      error: (error) => {
        this.notificationService.error('Unable to update status', this.getErrorMessage(error));
      }
    });
  }

  getInitials(fullName: string): string {
    const initials = fullName
      .split(' ')
      .filter(Boolean)
      .map((name) => name[0])
      .join('')
      .toUpperCase();

    return initials.slice(0, 2) || 'U';
  }

  isCurrentUser(userId: string): boolean {
    return this.currentUser?.id === userId;
  }

  getRoleStyles(role: Role): Record<string, string> {
    const config = this.roleConfig[role];
    return {
      background: config.bg,
      color: config.color
    };
  }

  getAvatarStyles(user: AuthUser): Record<string, string> {
    return {
      background: user.isActive === false ? '#9ca3af' : this.avatarColors[user.role],
      opacity: user.isActive === false ? '0.6' : '1'
    };
  }

  getStatusLabel(user: AuthUser): string {
    return user.isActive === false ? 'Inactive' : 'Active';
  }

  getJoinedLabel(createdAt: string): string {
    const parsedDate = new Date(createdAt);
    if (Number.isNaN(parsedDate.getTime())) {
      return '—';
    }

    return new Intl.DateTimeFormat('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    }).format(parsedDate);
  }

  navigateToAdrs(): void {
    void this.router.navigateByUrl('/adrs');
  }

  trackByUserId(_: number, user: AuthUser): string {
    return user.id;
  }

  private getErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const apiError = error.error as { message?: string } | null;
      return apiError?.message || error.message || 'Please try again.';
    }

    return 'Please try again.';
  }
}

import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectorRef, Component, DestroyRef, HostListener, NgZone, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { AuthUser, Role, WorkspaceInvitation } from '../../../models/auth.models';
import { AuthService } from '../../../services/auth.service';
import { ConfirmService } from '../../../services/confirm.service';
import { NotificationService } from '../../../services/notification.service';
import { AxiomLogoComponent } from '../../../shared/axiom-logo/axiom-logo.component';
import { WorkspaceEventsService } from '../../../services/workspace-events.service';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [CommonModule, FormsModule, AxiomLogoComponent],
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.scss']
})
export class UserManagementComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly notificationService = inject(NotificationService);
  private readonly confirmService = inject(ConfirmService);
  private readonly ngZone = inject(NgZone);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);
  private readonly workspaceEvents = inject(WorkspaceEventsService);

  users: AuthUser[] = [];
  filteredUsers: AuthUser[] = [];
  invitations: WorkspaceInvitation[] = [];
  searchQuery = '';
  roleFilter: Role | 'ALL' = 'ALL';
  openDropdownId: string | null = null;
  currentUser = this.authService.getCurrentUser();
  isLoading = false;
  showInviteModal = false;
  inviteEmail = '';
  inviteRole: Role = Role.REVIEWER;
  isInviting = false;
  latestInviteLink = '';
  inviteError = '';
  isLoadingInvitations = false;
  private lastSuccessfulInviteSignature = '';

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
    this.loadInvitations();
    this.workspaceEvents.events$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(event => {
      if (['INVITATION_ACCEPTED', 'INVITATION_CREATED', 'MEMBER_UPDATED'].includes(event.type)) {
        this.loadUsers();
        this.loadInvitations();
      }
    });
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
        this.ngZone.run(() => {
          this.users = users;
          this.applyFilter();
          this.isLoading = false;
          this.cdr.detectChanges();
        });
      },
      error: (error) => {
        this.ngZone.run(() => {
          this.isLoading = false;
          this.cdr.detectChanges();
          this.notificationService.error('Unable to load users', this.getErrorMessage(error));
        });
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

  async changeRole(userId: string, newRole: Role): Promise<void> {
    const existingUser = this.users.find((u) => u.id === userId);
    if (this.isCurrentUser(userId) || existingUser?.role === newRole) {
      this.openDropdownId = null;
      return;
    }

    this.openDropdownId = null;

    const confirmed = await this.confirmService.confirm({
      title: 'Change user role',
      message: `Change ${existingUser?.fullName ?? 'this user'}\'s role to ${newRole}? This will affect their permissions immediately.`,
      confirmLabel: 'Change Role',
      cancelLabel: 'Cancel',
      danger: false
    });
    if (!confirmed) return;

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
    this.latestInviteLink = '';
    this.inviteError = '';
    this.lastSuccessfulInviteSignature = '';
    this.showInviteModal = true;
  }

  closeInviteModal(): void {
    if (this.isInviting) return;
    this.showInviteModal = false;
  }

  get isInviteSentForCurrentInput(): boolean {
    return this.currentInviteSignature === this.lastSuccessfulInviteSignature;
  }

  get canSubmitInvite(): boolean {
    return !this.isInviting && !!this.inviteEmail.trim() && !this.isInviteSentForCurrentInput;
  }

  get inviteButtonLabel(): string {
    if (this.isInviting) return 'Sending...';
    if (this.isInviteSentForCurrentInput) return 'Invite sent ✓';
    return 'Send invite';
  }

  onInviteEmailChange(email: string): void {
    this.inviteEmail = email;
    this.inviteError = '';
  }

  onInviteRoleChange(role: Role): void {
    this.inviteRole = role;
    this.inviteError = '';
  }

  submitInvite(): void {
    const email = this.inviteEmail.trim().toLowerCase();
    if (!email || this.isInviting || this.isInviteSentForCurrentInput) return;

    const role = this.inviteRole;
    this.inviteError = '';
    this.isInviting = true;
    this.authService.inviteUser(email, role).subscribe({
      next: (response) => {
        this.isInviting = false;
        this.lastSuccessfulInviteSignature = this.buildInviteSignature(email, role);
        this.latestInviteLink = response.inviteLink;
        this.notificationService.success(
          'Invitation sent',
          `An invitation has been sent to ${email}.`
        );
        this.loadUsers();
        this.loadInvitations();
    this.workspaceEvents.events$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(event => {
      if (['INVITATION_ACCEPTED', 'INVITATION_CREATED', 'MEMBER_UPDATED'].includes(event.type)) {
        this.loadUsers();
        this.loadInvitations();
      }
    });
      },
      error: (error) => {
        this.isInviting = false;
        this.inviteError = this.getErrorMessage(error);
        this.notificationService.error('Invitation failed', this.inviteError);
      }
    });
  }

  loadInvitations(): void {
    this.isLoadingInvitations = true;
    this.authService.getWorkspaceInvitations().subscribe({
      next: (invitations) => {
        this.invitations = invitations;
        this.isLoadingInvitations = false;
      },
      error: (error) => {
        this.isLoadingInvitations = false;
        this.notificationService.error('Unable to load invitations', this.getErrorMessage(error));
      }
    });
  }

  async copyInviteLink(link = this.latestInviteLink): Promise<void> {
    if (!link) return;

    try {
      await navigator.clipboard.writeText(link);
      this.notificationService.success('Invite link copied', 'The invitation link is ready to share.');
    } catch {
      this.notificationService.error('Copy failed', 'Please copy the link manually.');
    }
  }

  async deactivateUser(user: AuthUser): Promise<void> {
    this.openDropdownId = null;
    const newStatus = !user.isActive;
    const userName = user.fullName || user.email;

    const confirmed = await this.confirmService.confirm({
      title: newStatus ? 'Activate account' : 'Deactivate account',
      message: newStatus
        ? `Reactivate ${userName}? They will be able to sign in again and continue using their existing workspace access.`
        : `Deactivate ${userName}? They will be unable to sign in, but their ADRs, votes, comments, and audit history will remain visible with authorship preserved.`,
      confirmLabel: newStatus ? 'Activate account' : 'Deactivate account',
      cancelLabel: 'Cancel',
      danger: !newStatus
    });
    if (!confirmed) return;

    this.authService.updateUserStatus(user.id, newStatus).subscribe({
      next: () => {
        const label = newStatus ? 'activated' : 'deactivated';
        this.notificationService.success(
          `User ${label}`,
          `${userName} has been ${label}.`
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
    return user.isActive === false ? 'Account disabled' : 'Active';
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

  getInvitationDateLabel(value: string): string {
    return this.getJoinedLabel(value);
  }

  getInvitationStatusLabel(invitation: WorkspaceInvitation): string {
    switch (invitation.status) {
      case 'ACCEPTED':
        return 'Accepted';
      case 'EXPIRED':
        return 'Expired';
      default:
        return 'Pending';
    }
  }

  getInvitationStatusClass(invitation: WorkspaceInvitation): string {
    return invitation.status.toLowerCase();
  }

  trackByInvitationId(_: number, invitation: WorkspaceInvitation): string {
    return invitation.tokenId;
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

  private get currentInviteSignature(): string {
    return this.buildInviteSignature(this.inviteEmail.trim().toLowerCase(), this.inviteRole);
  }

  private buildInviteSignature(email: string, role: Role): string {
    return `${email}|${role}`;
  }
}

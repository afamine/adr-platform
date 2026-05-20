import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, HostListener, NgZone, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import { AdrEditorComponent } from './components/adr-editor/adr-editor.component';
import { AdrSidebarComponent } from './components/adr-sidebar/adr-sidebar.component';
import { AiAssistantPanelComponent } from './components/ai-assistant-panel/ai-assistant-panel.component';
import { CollaborationPanelComponent } from './components/collaboration-panel/collaboration-panel.component';
import { VoteModalComponent } from './components/vote-modal/vote-modal.component';
import { ChangePasswordModalComponent } from '../../components/change-password-modal/change-password-modal.component';
import { allowedTransitions, Adr, AdrStatus, CastVoteRequest, CreateAdrRequest, UpdateAdrRequest, VoteDto } from '../../models/adr.model';
import { AuthService } from '../../services/auth.service';
import { AdrService } from '../../services/adr.service';
import { ConfirmService } from '../../services/confirm.service';
import { NotificationService } from '../../services/notification.service';
import { NotificationCenterService } from '../../services/notification-center.service';
import { BellNotification, NotificationApiDto } from '../../models/notification.models';

@Component({
  selector: 'app-adr-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, AdrSidebarComponent, AdrEditorComponent, AiAssistantPanelComponent, CollaborationPanelComponent, VoteModalComponent, ChangePasswordModalComponent],
  templateUrl: './adr-dashboard.component.html',
  styleUrl: './adr-dashboard.component.scss'
})
export class AdrDashboardComponent implements OnInit {
  private readonly adrService = inject(AdrService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly ngZone = inject(NgZone);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly notificationService = inject(NotificationService);
  private readonly notifCenterService = inject(NotificationCenterService);
  private readonly confirmService = inject(ConfirmService);

  adrs: Adr[] = [];
  filteredAdrs: Adr[] = [];
  selectedAdr: Adr | null = null;
  editingAdr: Partial<Adr> | null = null;
  activeTab: 'context' | 'decision' | 'consequences' | 'alternatives' | 'audit' = 'context';
  searchQuery = '';
  statusFilter: AdrStatus | 'ALL' = 'ALL';
  showAIPanel = false;
  showCollabPanel = false;
  isSaving = false;
  isEditing = false;
  isLoading = false;
  showSettings = false;
  showProfile = false;
  showNotifications = false;
  showChangePassword = false;

  activeNotifTab: 'all' | 'unread' = 'all';

  notifications: BellNotification[] = [];
  isLoadingNotifs = false;
  showVoteModal = false;
  voteModalAdr: Adr | null = null;
  voteModalVotes: VoteDto[] = [];
  voteModalMyVote: VoteDto | null = null;
  isVoteModalLoading = false;
  isVoteSubmitting = false;
  auditRefreshToken = 0;
  emailNotifications = true;
  currentUser = this.authService.getCurrentUser();

  get canCreateAdr(): boolean {
    const role = this.currentUser?.role;
    return role === 'AUTHOR' || role === 'ADMIN';
  }

  ngOnInit(): void {
    this.loadAdrs();
    this.loadNotifications();
  }

  onSearch(query: string): void {
    this.searchQuery = query;
    this.applyLocalFilters();
  }

  onFilterChange(status: AdrStatus | 'ALL'): void {
    this.statusFilter = status;
    this.applyLocalFilters();
  }

  private applyLocalFilters(): void {
    let result = [...this.adrs];
    if (this.statusFilter !== 'ALL') {
      result = result.filter((a) => a.status === this.statusFilter);
    }
    if (this.searchQuery.trim()) {
      const q = this.searchQuery.toLowerCase();
      result = result.filter(
        (a) =>
          a.title?.toLowerCase().includes(q) ||
          a.tags.some((t) => t.toLowerCase().includes(q))
      );
    }
    this.ngZone.run(() => {
      this.filteredAdrs = result;
      this.cdr.detectChanges();
    });
  }

  onSelectAdr(adr: Adr): void {
    this.selectedAdr = { ...adr };
    this.editingAdr = null;
    this.isEditing = false;
    this.activeTab = 'context';
  }

  onCreateNew(): void {
    this.isEditing = true;
    this.editingAdr = { title: '', tags: [], status: 'DRAFT' };
    this.selectedAdr = null;
    this.activeTab = 'context';
  }

  onSaveCreate(body: CreateAdrRequest): void {
    this.isSaving = true;
    const payload: CreateAdrRequest = {
      ...body,
      tags: body.tags ?? []
    };
    this.adrService
      .createAdr(payload)
      .pipe(finalize(() => (this.isSaving = false)))
      .subscribe({
        next: (created) => {
          this.notificationService.success('ADR created', `ADR-${created.adrNumber} was saved as a draft.`);
          this.selectedAdr = created;
          this.isEditing = false;
          this.editingAdr = null;
          this.loadAdrs(created.id);
        },
        error: (err) => this.handleError(err)
      });
  }

  private loadVoteModalData(adrId: string): void {
    this.isVoteModalLoading = true;

    forkJoin({
      votes: this.adrService.getVotes(adrId),
      myVote: this.adrService.getMyVote(adrId)
    }).subscribe({
      next: ({ votes, myVote }) => {
        this.ngZone.run(() => {
          this.voteModalVotes = votes;
          this.voteModalMyVote = myVote;
          this.isVoteModalLoading = false;
          this.cdr.detectChanges();
        });
      },
      error: () => {
        this.ngZone.run(() => {
          this.voteModalVotes = [];
          this.voteModalMyVote = null;
          this.isVoteModalLoading = false;
          this.notificationService.error('Failed to load votes for this ADR.');
          this.cdr.detectChanges();
        });
      }
    });
  }

  private refreshAdrState(adrId: string): void {
    this.adrService.getAdr(adrId).subscribe({
      next: (updatedAdr) => {
        this.selectedAdr = this.selectedAdr?.id === updatedAdr.id ? updatedAdr : this.selectedAdr;
        this.voteModalAdr = this.voteModalAdr?.id === updatedAdr.id ? updatedAdr : this.voteModalAdr;
        this.adrs = this.adrs.map((adr) => (adr.id === updatedAdr.id ? updatedAdr : adr));
        this.filteredAdrs = this.filteredAdrs.map((adr) => (adr.id === updatedAdr.id ? updatedAdr : adr));
      },
      error: () => {
        this.loadAdrs(adrId);
      }
    });
  }

  private handleVoteError(err: { status?: number; message?: string; errorType?: string }, adrId: string): void {
    if (err.errorType === 'ALREADY_VOTED') {
      this.notificationService.info('You already voted on this ADR.');
      this.loadVoteModalData(adrId);
      return;
    }

    if (err.errorType === 'INVALID_VOTE') {
      this.notificationService.warning('This ADR is not under review.');
      return;
    }

    if (err.errorType === 'ACCESS_DENIED') {
      this.notificationService.error('Authors cannot vote on ADRs.');
      return;
    }

    this.notificationService.error(err.message || 'Failed to submit vote. Please try again.');
  }

  onSaveUpdate(body: UpdateAdrRequest): void {
    if (!this.selectedAdr) {
      return;
    }

    this.isSaving = true;
    const payload: UpdateAdrRequest = {
      ...body,
      tags: body.tags ?? []
    };
    this.adrService
      .updateAdr(this.selectedAdr.id, payload)
      .pipe(finalize(() => (this.isSaving = false)))
      .subscribe({
        next: (updated) => {
          this.isEditing = false;
          this.editingAdr = null;
          this.notificationService.success('ADR saved', 'Your changes have been saved.');
          this.selectedAdr = updated;
          this.loadAdrs(updated.id);
        },
        error: (err) => this.handleError(err)
      });
  }

  onTransitionStatus(newStatus: AdrStatus): void {
    if (!this.selectedAdr) {
      return;
    }

    this.adrService.transitionStatus(this.selectedAdr.id, newStatus).subscribe({
      next: (updated) => {
        this.selectedAdr = updated;
        this.adrs = this.adrs.map((adr) => (adr.id === updated.id ? updated : adr));
        this.filteredAdrs = this.filteredAdrs.map((adr) => (adr.id === updated.id ? updated : adr));
        this.notificationService.success('Status updated', `The ADR is now ${updated.status}.`);
      },
      error: (err) => this.handleError(err, true)
    });
  }

  onStatusChange(newStatus: AdrStatus): void {
    if (!this.selectedAdr || newStatus === this.selectedAdr.status) {
      return;
    }

    this.onTransitionStatus(newStatus);
  }

  async onDelete(): Promise<void> {
    if (!this.selectedAdr) return;

    const confirmed = await this.confirmService.confirm({
      title: 'Delete ADR',
      message: `Delete "${this.selectedAdr.title}"? This action cannot be undone.`,
      confirmLabel: 'Delete permanently',
      cancelLabel: 'Cancel',
      danger: true
    });
    if (!confirmed) return;

    const deletedId = this.selectedAdr.id;
    this.adrService.deleteAdr(deletedId).subscribe({
      next: () => {
        this.adrs = this.adrs.filter((adr) => adr.id !== deletedId);
        this.filteredAdrs = this.filteredAdrs.filter((adr) => adr.id !== deletedId);
        this.selectedAdr = this.filteredAdrs[0] ?? this.adrs[0] ?? null;
        this.isEditing = false;
        this.editingAdr = null;
        this.notificationService.success('ADR deleted');
      },
      error: (err) => this.handleError(err, true)
    });
  }

  onEditCurrent(): void {
    if (!this.selectedAdr) {
      return;
    }

    this.isEditing = true;
    this.editingAdr = { ...this.selectedAdr };
  }

  onCancelEdit(): void {
    this.isEditing = false;
    this.editingAdr = null;

    if (!this.selectedAdr) {
      this.selectedAdr = this.filteredAdrs[0] ?? this.adrs[0] ?? null;
    }
  }

  onTabChange(tab: 'context' | 'decision' | 'consequences' | 'alternatives' | 'audit'): void {
    this.activeTab = tab;
  }

  toggleAI(): void {
    this.showAIPanel = !this.showAIPanel;
    if (this.showAIPanel) {
      this.showCollabPanel = false;
    }
  }

  toggleCollab(): void {
    this.showCollabPanel = !this.showCollabPanel;
    if (this.showCollabPanel) {
      this.showAIPanel = false;
    }
  }

  get currentUserName(): string {
    return this.currentUser?.fullName ?? 'You';
  }

  get unreadCount(): number {
    return this.notifications.filter((n) => n.unread).length;
  }

  get displayedNotifications() {
    return this.activeNotifTab === 'unread'
      ? this.notifications.filter((n) => n.unread)
      : this.notifications;
  }

  toggleSettings(event?: Event): void {
    event?.stopPropagation();
    this.showSettings = !this.showSettings;
    this.showProfile = false;
    this.showNotifications = false;
  }

  toggleNotifications(event?: Event): void {
    event?.stopPropagation();
    this.showNotifications = !this.showNotifications;
    this.showSettings = false;
    this.showProfile = false;
    if (this.showNotifications) this.loadNotifications();
  }

  viewAllNotifications(): void {
    this.showNotifications = false;
    void this.router.navigate(['/notifications']);
  }

  markAllRead(): void {
    const previous = this.notifications.map(n => n.unread);
    this.notifications.forEach(n => (n.unread = false));
    this.notifCenterService.markAllRead().subscribe({
      error: () => this.notifications.forEach((n, i) => (n.unread = previous[i]))
    });
  }

  markNotifRead(id: string): void {
    const n = this.notifications.find(n => n.id === id);
    if (!n || !n.unread) return;
    n.unread = false;
    this.notifCenterService.markRead(id).subscribe({
      error: () => { if (n) n.unread = true; }
    });
  }

  loadNotifications(): void {
    this.isLoadingNotifs = true;
    this.notifCenterService.getNotifications(20, false).subscribe({
      next: (items) => {
        this.notifications = items.map(dto => this.mapNotif(dto));
        this.isLoadingNotifs = false;
      },
      error: () => {
        this.isLoadingNotifs = false;
      }
    });
  }

  private mapNotif(dto: NotificationApiDto): BellNotification {
    const dotColors: Record<string, string> = {
      ADR_SUBMITTED_FOR_REVIEW: '#ba7517',
      VOTE_CAST_ON_MY_ADR:      '#6366f1',
      ADR_STATUS_CHANGED:       '#1d9e75',
      ADR_REJECTED:             '#ef4444',
      NEW_TEAM_MEMBER:          '#9ca3af',
      COMMENT_ADDED:            '#9ca3af'
    };
    const actions: Record<string, string> = {
      ADR_SUBMITTED_FOR_REVIEW: 'Review ADR',
      VOTE_CAST_ON_MY_ADR:      '',
      ADR_STATUS_CHANGED:       'View ADR',
      ADR_REJECTED:             'View ADR',
      NEW_TEAM_MEMBER:          '',
      COMMENT_ADDED:            'View ADR'
    };
    return {
      id:       dto.id,
      type:     dto.type,
      dotColor: dotColors[dto.type] ?? '#9ca3af',
      title:    dto.title,
      body:     dto.body,
      time:     dto.timeAgo,
      action:   actions[dto.type] ?? '',
      adrId:    dto.adrId,
      unread:   !dto.isRead
    };
  }

  toggleProfile(event?: Event): void {
    event?.stopPropagation();
    this.showProfile = !this.showProfile;
    this.showSettings = false;
    this.showNotifications = false;
  }

  onMyProfile(): void {
    this.showProfile = false;
    void this.router.navigate(['/profile']);
  }

  onChangePassword(): void {
    this.showProfile = false;
    this.showChangePassword = true;
  }

  openVoteModal(adr: Adr): void {
    if (!this.canVoteOnAdr(adr)) {
      return;
    }

    this.voteModalAdr = adr;
    this.voteModalVotes = [];
    this.voteModalMyVote = null;
    this.showVoteModal = true;
    this.loadVoteModalData(adr.id);
  }

  onVoteSubmitted(event: CastVoteRequest): void {
    if (!this.voteModalAdr) {
      return;
    }

    const adrId = this.voteModalAdr.id;
    this.isVoteSubmitting = true;

    this.adrService
      .castVote(adrId, event)
      .pipe(finalize(() => (this.isVoteSubmitting = false)))
      .subscribe({
        next: (createdVote) => {
          this.voteModalMyVote = createdVote;
          this.notificationService.success('Vote submitted successfully');
          this.refreshAdrState(adrId);
          this.loadVoteModalData(adrId);
          this.auditRefreshToken += 1;
        },
        error: (err) => this.handleVoteError(err, adrId)
      });
  }

  closeVoteModal(): void {
    this.showVoteModal = false;
    this.voteModalAdr = null;
    this.voteModalVotes = [];
    this.voteModalMyVote = null;
    this.isVoteModalLoading = false;
    this.isVoteSubmitting = false;
  }

  onCloseChangePassword(): void {
    this.showChangePassword = false;
  }

  onSignOut(): void {
    this.showProfile = false;
    this.authService.logout();
  }

  get userInitials(): string {
    if (!this.currentUser?.fullName) {
      return '?';
    }

    return this.currentUser.fullName
      .split(' ')
      .map((part) => part[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  }

  canEdit(): boolean {
    if (!this.selectedAdr || !this.currentUser) return false;
    if (this.currentUser.role === 'ADMIN') return true;
    return this.currentUser.role === 'AUTHOR'
      && this.currentUser.id === this.selectedAdr.authorId
      && ['DRAFT','PROPOSED'].includes(this.selectedAdr.status);
  }

  getAllowedTransitions(): AdrStatus[] {
    if (!this.selectedAdr || !this.currentUser) {
      return [];
    }

    return allowedTransitions(this.selectedAdr, this.currentUser);
  }

  canVoteOnAdr(adr: Adr | null): boolean {
    if (!adr || adr.status !== 'UNDER_REVIEW') {
      return false;
    }

    const role = this.currentUser?.role;
    return role === 'REVIEWER' || role === 'APPROVER' || role === 'ADMIN';
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: Event): void {
    const target = event.target as HTMLElement | null;
    if (target?.closest('.adr-dashboard__dropdown-anchor')) {
      return;
    }

    this.showSettings = false;
    this.showProfile = false;
    this.showNotifications = false;
  }

  private loadAdrs(selectId?: string): void {
    this.isLoading = true;

    this.adrService.getAdrs({ size: 20, page: 0 }).subscribe({
      next: (adrs) => {
        this.ngZone.run(() => {
          this.adrs = adrs;
          this.isLoading = false;

          let filtered = [...adrs];
          if (this.statusFilter !== 'ALL') {
            filtered = filtered.filter((a) => a.status === this.statusFilter);
          }
          if (this.searchQuery.trim()) {
            const q = this.searchQuery.toLowerCase();
            filtered = filtered.filter(
              (a) => a.title?.toLowerCase().includes(q) || a.tags.some((t) => t.toLowerCase().includes(q))
            );
          }
          this.filteredAdrs = filtered;

          const preferredId = selectId ?? this.selectedAdr?.id;
          const nextSelected = preferredId ? adrs.find((adr) => adr.id === preferredId) ?? null : null;

          if (nextSelected) {
            this.onSelectAdr(nextSelected);
            this.cdr.detectChanges();
            return;
          }

          if (adrs.length > 0 && (!this.selectedAdr || !adrs.some((adr) => adr.id === this.selectedAdr?.id))) {
            this.onSelectAdr(adrs[0]);
            this.cdr.detectChanges();
            return;
          }

          if (adrs.length === 0) {
            this.selectedAdr = null;
            this.editingAdr = null;
          }

          this.cdr.detectChanges();
        });
      },
      error: (err) => {
        this.ngZone.run(() => {
          console.error('Failed to load ADRs:', err);
          this.isLoading = false;
          this.notificationService.error('Loading failed', 'Unable to load ADRs.');
          this.adrs = [];
          this.filteredAdrs = [];
          this.selectedAdr = null;
          this.cdr.detectChanges();
        });
      }
    });
  }

  private handleError(err: { status?: number; message?: string; errorType?: string }, reloadOnNotFound = false): void {
    if (err.status === 0) {
      this.notificationService.error('Failed to connect to server. Please try again.');
      return;
    }

    if (err.status === 400) {
      this.notificationService.warning(err.message || 'An error occurred');
      return;
    }

    if (err.status === 403) {
      this.notificationService.error("You don't have permission to modify this ADR.");
      return;
    }

    if (err.status === 404) {
      this.notificationService.error('ADR not found');
      if (reloadOnNotFound) {
        this.loadAdrs();
      }
      return;
    }

    this.notificationService.error(err.message || 'An error occurred');
  }
}

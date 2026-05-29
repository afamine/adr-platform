import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, DestroyRef, HostListener, NgZone, OnInit, computed, inject } from '@angular/core';
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
import { AdrStateService } from './services/adr-state.service';

@Component({
  selector: 'app-adr-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, AdrSidebarComponent, AdrEditorComponent, AiAssistantPanelComponent, CollaborationPanelComponent, VoteModalComponent, ChangePasswordModalComponent],
  templateUrl: './adr-dashboard.component.html',
  styleUrl: './adr-dashboard.component.scss'
})
export class AdrDashboardComponent implements OnInit {
  private readonly adrService = inject(AdrService);
  private readonly adrState = inject(AdrStateService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly ngZone = inject(NgZone);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly notificationService = inject(NotificationService);
  private readonly notifCenterService = inject(NotificationCenterService);
  private readonly confirmService = inject(ConfirmService);
  private readonly destroyRef = inject(DestroyRef);
  private searchDebounceTimer: any = null;

  readonly liveUnreadCount$ = this.notifCenterService.unreadCount$;

  readonly adrs = computed(() => this.adrState.adrs$());
  readonly selectedAdr = computed(() => this.adrState.selectedAdr$());
  readonly isLoading = computed(() => this.adrState.isLoading$());
  readonly currentPage = computed(() => this.adrState.currentPage$());
  readonly totalElements = computed(() => this.adrState.totalElements$());
  readonly totalPages = computed(() => this.adrState.totalPages$());
  readonly searchQuery = computed(() => this.adrState.searchQuery$());
  readonly statusFilter = computed(() => this.adrState.statusFilter$());

  editingAdr: Partial<Adr> | null = null;
  activeTab: 'context' | 'decision' | 'consequences' | 'alternatives' | 'audit' = 'context';
  showAIPanel = false;
  showCollabPanel = false;
  isSaving = false;
  isEditing = false;
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
    this.notifCenterService.startPolling(this.destroyRef);
  }

  onSearch(query: string): void {
    this.adrState.searchQuery$.set(query);
    if (this.searchDebounceTimer) clearTimeout(this.searchDebounceTimer);
    this.searchDebounceTimer = setTimeout(() => {
      this.adrState.currentPage$.set(0);
      this.loadAdrs();
    }, 300);
  }

  onFilterChange(status: AdrStatus | 'ALL'): void {
    this.adrState.statusFilter$.set(status);
    this.adrState.currentPage$.set(0);
    this.loadAdrs();
  }

  onPreviousPage(): void {
    if (this.currentPage() > 0) {
      this.adrState.currentPage$.update((page) => page - 1);
      this.loadAdrs();
    }
  }

  onNextPage(): void {
    if (this.currentPage() < this.totalPages() - 1) {
      this.adrState.currentPage$.update((page) => page + 1);
      this.loadAdrs();
    }
  }

  onSelectAdr(adr: Adr): void {
    this.adrState.selectAdr(adr);
    this.editingAdr = null;
    this.isEditing = false;
    this.activeTab = 'context';
  }

  onCreateNew(): void {
    this.isEditing = true;
    this.editingAdr = { title: '', tags: [], status: 'DRAFT' };
    this.adrState.clearSelection();
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
          this.adrState.selectAdr(created);
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
        if (this.selectedAdr()?.id === updatedAdr.id) {
          this.adrState.selectAdr(updatedAdr);
        }
        this.voteModalAdr = this.voteModalAdr?.id === updatedAdr.id ? updatedAdr : this.voteModalAdr;
        this.adrState.updateAdrInList(updatedAdr);
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
    const selected = this.selectedAdr();
    if (!selected) {
      return;
    }

    this.isSaving = true;
    const payload: UpdateAdrRequest = {
      ...body,
      tags: body.tags ?? []
    };
    this.adrService
      .updateAdr(selected.id, payload)
      .pipe(finalize(() => (this.isSaving = false)))
      .subscribe({
        next: (updated) => {
          this.isEditing = false;
          this.editingAdr = null;
          this.notificationService.success('ADR saved', 'Your changes have been saved.');
          this.adrState.selectAdr(updated);
          this.loadAdrs(updated.id);
        },
        error: (err) => this.handleError(err)
      });
  }

  onTransitionStatus(newStatus: AdrStatus): void {
    const selected = this.selectedAdr();
    if (!selected) {
      return;
    }

    this.adrService.transitionStatus(selected.id, newStatus).subscribe({
      next: (updated) => {
        this.adrState.updateAdrInList(updated);
        this.notificationService.success('Status updated', `The ADR is now ${updated.status}.`);
      },
      error: (err) => this.handleError(err, true)
    });
  }

  onStatusChange(newStatus: AdrStatus): void {
    const selected = this.selectedAdr();
    if (!selected || newStatus === selected.status) {
      return;
    }

    this.onTransitionStatus(newStatus);
  }

  onEditorAdrUpdated(updated: Adr): void {
    this.adrState.updateAdrInList(updated);
    this.notificationService.success('Status updated', `The ADR is now ${updated.status}.`);
  }

  onLinkedAdrNavigate(adrId: string): void {
    const existing = this.adrs().find((adr) => adr.id === adrId);
    if (existing) {
      this.onSelectAdr(existing);
      return;
    }

    this.adrService.getAdr(adrId).subscribe({
      next: (adr) => this.onSelectAdr(adr),
      error: (err) => this.handleError(err)
    });
  }

  async onDelete(): Promise<void> {
    const selected = this.selectedAdr();
    if (!selected) return;

    const confirmed = await this.confirmService.confirm({
      title: 'Delete ADR',
      message: `Delete "${selected.title}"? This action cannot be undone.`,
      confirmLabel: 'Delete permanently',
      cancelLabel: 'Cancel',
      danger: true
    });
    if (!confirmed) return;

    const deletedId = selected.id;
    this.adrService.deleteAdr(deletedId).subscribe({
      next: () => {
        this.adrState.clearSelection();
        this.loadAdrs();
        this.isEditing = false;
        this.editingAdr = null;
        this.notificationService.success('ADR deleted');
      },
      error: (err) => this.handleError(err, true)
    });
  }

  onEditCurrent(): void {
    const selected = this.selectedAdr();
    if (!selected) {
      return;
    }

    this.isEditing = true;
    this.editingAdr = { ...selected };
  }

  onCancelEdit(): void {
    this.isEditing = false;
    this.editingAdr = null;

    if (!this.selectedAdr()) {
      const firstAdr = this.adrs()[0] ?? null;
      if (firstAdr) {
        this.adrState.selectAdr(firstAdr);
      }
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
      next: () => this.notifCenterService.fetchUnreadCount(),
      error: () => this.notifications.forEach((n, i) => (n.unread = previous[i]))
    });
  }

  markNotifRead(id: string): void {
    const n = this.notifications.find(n => n.id === id);
    if (!n || !n.unread) return;
    n.unread = false;
    this.notifCenterService.markRead(id).subscribe({
      next: () => this.notifCenterService.fetchUnreadCount(),
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
    const selected = this.selectedAdr();
    if (!selected || !this.currentUser) return false;
    if (this.currentUser.role === 'ADMIN') return true;
    return this.currentUser.role === 'AUTHOR'
      && this.currentUser.id === selected.authorId
      && ['DRAFT','PROPOSED'].includes(selected.status);
  }

  getAllowedTransitions(): AdrStatus[] {
    const selected = this.selectedAdr();
    if (!selected || !this.currentUser) {
      return [];
    }

    return allowedTransitions(selected, this.currentUser);
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
    this.adrState.loadAdrs(selectId);
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

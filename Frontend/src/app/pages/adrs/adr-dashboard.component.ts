import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, HostListener, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
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
import { NotificationService } from '../../services/notification.service';

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
  private readonly cdr = inject(ChangeDetectorRef);
  readonly notificationService = inject(NotificationService);

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
  showChangePassword = false;
  showVoteModal = false;
  voteModalAdr: Adr | null = null;
  voteModalVotes: VoteDto[] = [];
  voteModalMyVote: VoteDto | null = null;
  isVoteModalLoading = false;
  isVoteSubmitting = false;
  auditRefreshToken = 0;
  emailNotifications = true;
  currentUser = this.authService.getCurrentUser();

  ngOnInit(): void {
    this.loadAdrs();
  }

  onSearch(query: string): void {
    this.searchQuery = query;
    this.loadAdrs();
  }

  onFilterChange(status: AdrStatus | 'ALL'): void {
    this.statusFilter = status;
    this.loadAdrs();
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
          this.notificationService.success('ADR créé', `ADR-${created.adrNumber} enregistré en brouillon.`);
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
    })
      .pipe(finalize(() => (this.isVoteModalLoading = false)))
      .subscribe({
        next: ({ votes, myVote }) => {
          this.voteModalVotes = votes;
          this.voteModalMyVote = myVote;
        },
        error: () => {
          this.voteModalVotes = [];
          this.voteModalMyVote = null;
          this.notificationService.error('Failed to load votes for this ADR.');
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
          this.notificationService.success('ADR sauvegardé', 'Vos modifications ont été enregistrées.');
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
        this.notificationService.success('Statut mis à jour', `L'ADR est maintenant ${updated.status}.`);
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

  onDelete(): void {
    if (!this.selectedAdr || !window.confirm('Are you sure you want to delete this ADR?')) {
      return;
    }

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

  toggleSettings(event?: Event): void {
    event?.stopPropagation();
    this.showSettings = !this.showSettings;
    this.showProfile = false;
  }

  toggleProfile(event?: Event): void {
    event?.stopPropagation();
    this.showProfile = !this.showProfile;
    this.showSettings = false;
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
  }

  private loadAdrs(selectId?: string): void {
    this.isLoading = true;
    const params = {
      status: this.statusFilter === 'ALL' ? undefined : this.statusFilter,
      search: this.searchQuery.trim() || undefined
    };

    this.adrService.getAdrs(params).subscribe({
      next: (adrs) => {
        this.adrs = adrs;
        this.filteredAdrs = [...adrs];
        this.isLoading = false;

        const preferredId = selectId ?? this.selectedAdr?.id;
        const nextSelected = preferredId ? adrs.find((adr) => adr.id === preferredId) ?? null : null;

        if (nextSelected) {
          this.onSelectAdr(nextSelected);
          return;
        }

        if (adrs.length > 0 && (!this.selectedAdr || !adrs.some((adr) => adr.id === this.selectedAdr?.id))) {
          this.onSelectAdr(adrs[0]);
          return;
        }

        if (adrs.length === 0) {
          this.selectedAdr = null;
          this.editingAdr = null;
        }

        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load ADRs:', err);
        this.isLoading = false;
        this.notificationService.error('Chargement échoué', 'Impossible de charger les ADRs.');
        this.adrs = [];
        this.filteredAdrs = [];
        this.selectedAdr = null;

        this.cdr.detectChanges();
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

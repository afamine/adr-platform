import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, HostListener, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AdrEditorComponent } from './components/adr-editor/adr-editor.component';
import { AdrSidebarComponent } from './components/adr-sidebar/adr-sidebar.component';
import { AiAssistantPanelComponent } from './components/ai-assistant-panel/ai-assistant-panel.component';
import { CollaborationPanelComponent } from './components/collaboration-panel/collaboration-panel.component';
import { ChangePasswordModalComponent } from '../../components/change-password-modal/change-password-modal.component';
import { allowedTransitions, Adr, AdrStatus, CreateAdrRequest, UpdateAdrRequest } from '../../models/adr.model';
import { AuthService } from '../../services/auth.service';
import { AdrService } from '../../services/adr.service';
import { NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-adr-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, AdrSidebarComponent, AdrEditorComponent, AiAssistantPanelComponent, CollaborationPanelComponent, ChangePasswordModalComponent],
  templateUrl: './adr-dashboard.component.html',
  styleUrl: './adr-dashboard.component.scss'
})
export class AdrDashboardComponent implements OnInit {
  private readonly adrService = inject(AdrService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);
  readonly notificationService = inject(NotificationService);

  adrs: Adr[] = [];
  filteredAdrs: Adr[] = [];
  selectedAdr: Adr | null = null;
  editingAdr: Partial<Adr> | null = null;
  activeTab: 'context' | 'decision' | 'consequences' | 'alternatives' = 'context';
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
    this.adrService
      .createAdr(body)
      .pipe(finalize(() => (this.isSaving = false)))
      .subscribe({
        next: (created) => {
          this.notificationService.success('ADR created');
          this.isEditing = false;
          this.editingAdr = null;
          this.loadAdrs(created.id);
        },
        error: (err) => this.handleError(err)
      });
  }

  onSaveUpdate(body: UpdateAdrRequest): void {
    if (!this.selectedAdr) {
      return;
    }

    this.isSaving = true;
    this.adrService
      .updateAdr(this.selectedAdr.id, body)
      .pipe(finalize(() => (this.isSaving = false)))
      .subscribe({
        next: (updated) => {
          this.selectedAdr = updated;
          this.adrs = this.adrs.map((adr) => (adr.id === updated.id ? updated : adr));
          this.filteredAdrs = this.filteredAdrs.map((adr) => (adr.id === updated.id ? updated : adr));
          this.isEditing = false;
          this.editingAdr = null;
          this.notificationService.success('ADR updated');
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
        this.notificationService.success(`Status changed to ${newStatus.replaceAll('_', ' ')}`);
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

  onTabChange(tab: 'context' | 'decision' | 'consequences' | 'alternatives'): void {
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

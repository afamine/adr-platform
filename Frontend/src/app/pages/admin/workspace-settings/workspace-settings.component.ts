import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService } from '../../../services/auth.service';
import { NotificationService } from '../../../services/notification.service';
import { WorkspaceService } from '../../../services/workspace.service';

@Component({
  selector: 'app-workspace-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './workspace-settings.component.html',
  styleUrls: ['./workspace-settings.component.scss']
})
export class WorkspaceSettingsComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly notificationService = inject(NotificationService);
  private readonly workspaceService = inject(WorkspaceService);

  activeTab: 'general' | 'notifications' | 'integrations' = 'general';

  workspaceName = '';
  workspaceSlug = '';
  quorum = 2;
  quorumMode: 'auto' | 'manual' = 'auto';
  isLoading = false;
  isSaving = false;

  readonly currentUser = this.authService.getCurrentUser();

  ngOnInit(): void {
    this.loadWorkspace();
  }

  loadWorkspace(): void {
    this.isLoading = true;
    this.workspaceService.getWorkspace().subscribe({
      next: (ws) => {
        this.workspaceName = ws.name;
        this.workspaceSlug = ws.slug;
        this.quorum = ws.voteQuorum;
        this.quorumMode = ws.quorumMode === 'MANUAL' ? 'manual' : 'auto';
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.notificationService.error('Failed to load workspace', this.getErrorMessage(err));
      }
    });
  }

  saveWorkspaceInfo(): void {
    this.isSaving = true;
    this.workspaceService.updateWorkspace({
      name: this.workspaceName.trim(),
      slug: this.workspaceSlug.trim().toLowerCase(),
      voteQuorum: this.quorum,
      quorumMode: this.quorumMode === 'manual' ? 'MANUAL' : 'AUTO'
    }).subscribe({
      next: (ws) => {
        this.workspaceName = ws.name;
        this.workspaceSlug = ws.slug;
        this.isSaving = false;
        this.notificationService.success('Workspace updated', 'Name and slug saved successfully.');
      },
      error: (err) => {
        this.isSaving = false;
        this.notificationService.error('Failed to save', this.getErrorMessage(err));
      }
    });
  }

  saveConfiguration(): void {
    this.isSaving = true;
    this.workspaceService.updateWorkspace({
      name: this.workspaceName.trim(),
      slug: this.workspaceSlug.trim().toLowerCase(),
      voteQuorum: this.quorum,
      quorumMode: this.quorumMode === 'manual' ? 'MANUAL' : 'AUTO'
    }).subscribe({
      next: (ws) => {
        this.quorum = ws.voteQuorum;
        this.quorumMode = ws.quorumMode === 'MANUAL' ? 'manual' : 'auto';
        this.isSaving = false;
        this.notificationService.success('Configuration saved', 'Vote settings updated successfully.');
      },
      error: (err) => {
        this.isSaving = false;
        this.notificationService.error('Failed to save', this.getErrorMessage(err));
      }
    });
  }

  resetWorkspace(): void {
    if (!confirm('Are you sure? This will reset all workspace settings to default values.')) return;
    this.isSaving = true;
    this.workspaceService.resetWorkspace().subscribe({
      next: () => {
        this.isSaving = false;
        this.notificationService.success('Workspace reset', 'Settings have been reset to defaults.');
        this.loadWorkspace();
      },
      error: (err) => {
        this.isSaving = false;
        this.notificationService.error('Failed to reset', this.getErrorMessage(err));
      }
    });
  }

  getInitials(): string {
    const name = this.currentUser?.fullName ?? '';
    return name
      .split(' ')
      .filter(Boolean)
      .map((n) => n[0])
      .join('')
      .toUpperCase()
      .slice(0, 2) || 'AD';
  }

  navigateTo(path: string): void {
    void this.router.navigate([path]);
  }

  private getErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const body = error.error as { message?: string } | null;
      return body?.message || error.message || 'Please try again.';
    }
    return 'Please try again.';
  }
}

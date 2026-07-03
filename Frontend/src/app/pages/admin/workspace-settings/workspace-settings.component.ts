import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AdminLayoutComponent } from '../../../layouts/admin-layout/admin-layout.component';
import { UpdateWorkspaceRequest, WorkspaceJoinPolicy } from '../../../models/auth.models';
import { NotificationService } from '../../../services/notification.service';
import { WorkspaceService } from '../../../services/workspace.service';

@Component({
  selector: 'app-workspace-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, AdminLayoutComponent],
  templateUrl: './workspace-settings.component.html',
  styleUrls: ['./workspace-settings.component.scss']
})
export class WorkspaceSettingsComponent implements OnInit {
  private readonly notificationService = inject(NotificationService);
  private readonly workspaceService = inject(WorkspaceService);

  activeTab: 'general' | 'notifications' | 'integrations' = 'general';

  workspaceName = '';
  workspaceSlug = '';
  joinPolicy: WorkspaceJoinPolicy = 'INVITE_ONLY';
  quorum = 2;
  quorumMode: 'auto' | 'manual' = 'auto';
  isLoading = false;
  isSaving = false;

  ngOnInit(): void {
    this.loadWorkspace();
  }

  loadWorkspace(): void {
    this.isLoading = true;
    this.workspaceService.getWorkspace().subscribe({
      next: (ws) => {
        this.workspaceName = ws.name;
        this.workspaceSlug = ws.slug;
        this.joinPolicy = ws.joinPolicy ?? 'INVITE_ONLY';
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
    this.workspaceService.updateWorkspace(this.buildUpdateRequest()).subscribe({
      next: (ws) => {
        this.workspaceName = ws.name;
        this.workspaceSlug = ws.slug;
        this.joinPolicy = ws.joinPolicy;
        this.isSaving = false;
        this.notificationService.success('Workspace updated', 'Workspace identity and joining policy saved.');
      },
      error: (err) => {
        this.isSaving = false;
        this.notificationService.error('Failed to save', this.getErrorMessage(err));
      }
    });
  }

  saveConfiguration(): void {
    this.isSaving = true;
    this.workspaceService.updateWorkspace(this.buildUpdateRequest()).subscribe({
      next: (ws) => {
        this.quorum = ws.voteQuorum;
        this.quorumMode = ws.quorumMode === 'MANUAL' ? 'manual' : 'auto';
        this.joinPolicy = ws.joinPolicy;
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

  setJoinPolicy(policy: WorkspaceJoinPolicy): void {
    this.joinPolicy = policy;
  }

  async copyWorkspaceSlug(): Promise<void> {
    const slug = this.workspaceSlug.trim().toLowerCase();
    if (!slug) {
      this.notificationService.warning('No slug to copy', 'Save a workspace slug first.');
      return;
    }

    try {
      await navigator.clipboard.writeText(slug);
      this.notificationService.success('Workspace slug copied', slug);
    } catch {
      this.notificationService.error('Copy failed', 'Please copy the slug manually.');
    }
  }

  private buildUpdateRequest(): UpdateWorkspaceRequest {
    return {
      name: this.workspaceName.trim(),
      slug: this.workspaceSlug.trim().toLowerCase(),
      voteQuorum: this.quorum,
      quorumMode: this.quorumMode === 'manual' ? 'MANUAL' : 'AUTO',
      joinPolicy: this.joinPolicy
    };
  }

  private getErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const body = error.error as { message?: string } | null;
      return body?.message || error.message || 'Please try again.';
    }
    return 'Please try again.';
  }
}
